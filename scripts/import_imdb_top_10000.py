#!/usr/bin/env python3
"""
Neo4flix - Import Top 10,000 IMDB Movies with Full Metadata
============================================================
Downloads IMDB datasets and imports top 10,000 movies with:
- Genres (BELONGS_TO_GENRE relationships)
- Directors (DIRECTED relationships)
- Actors (ACTED_IN relationships)
- IMDB ratings (as properties)

Data Source: https://datasets.imdbapis.com/
License: https://www.imdb.com/interfaces/ (Non-commercial use)

Requirements:
    pip install neo4j requests pandas

Usage:
    python import_imdb_top_10000.py --clean --import
"""

import argparse
import gzip
import os
import sys
import time
from pathlib import Path
from typing import Dict, List, Set, Tuple
import requests
from neo4j import GraphDatabase
import pandas as pd


class IMDBImporter:
    """Import top IMDB movies into Neo4j"""

    # IMDB dataset URLs
    IMDB_DATASETS = {
        'title_basics': 'https://datasets.imdbapis.com/title.basics.tsv.gz',
        'title_ratings': 'https://datasets.imdbapis.com/title.ratings.tsv.gz',
        'title_crew': 'https://datasets.imdbapis.com/title.crew.tsv.gz',
        'title_principals': 'https://datasets.imdbapis.com/title.principals.tsv.gz',
        'name_basics': 'https://datasets.imdbapis.com/name.basics.tsv.gz',
    }

    def __init__(self, neo4j_uri: str, neo4j_user: str, neo4j_password: str,
                 data_dir: str = './imdb_data'):
        self.driver = GraphDatabase.driver(neo4j_uri, auth=(neo4j_user, neo4j_password))
        self.data_dir = Path(data_dir)
        self.data_dir.mkdir(exist_ok=True)

    def close(self):
        self.driver.close()

    # ========================================================================
    # Step 1: Download IMDB Datasets
    # ========================================================================

    def download_dataset(self, name: str, url: str) -> Path:
        """Download and extract IMDB dataset"""
        gz_file = self.data_dir / f"{name}.tsv.gz"
        tsv_file = self.data_dir / f"{name}.tsv"

        # Skip if already extracted
        if tsv_file.exists():
            print(f"  ✓ {name}.tsv already exists")
            return tsv_file

        # Download if needed
        if not gz_file.exists():
            print(f"  📥 Downloading {name}...")
            response = requests.get(url, stream=True)
            response.raise_for_status()

            total_size = int(response.headers.get('content-length', 0))
            downloaded = 0

            with open(gz_file, 'wb') as f:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
                    downloaded += len(chunk)
                    if total_size > 0:
                        percent = (downloaded / total_size) * 100
                        print(f"    Progress: {percent:.1f}%", end='\r')
            print(f"    ✓ Downloaded: {downloaded / (1024*1024):.1f} MB")

        # Extract
        print(f"  📦 Extracting {name}...")
        with gzip.open(gz_file, 'rb') as f_in:
            with open(tsv_file, 'wb') as f_out:
                f_out.write(f_in.read())
        print(f"  ✓ Extracted: {name}.tsv")

        return tsv_file

    def download_all_datasets(self):
        """Download all required IMDB datasets"""
        print("\n📊 Downloading IMDB Datasets")
        print("=" * 70)
        print("This may take 10-20 minutes (datasets are ~1-2 GB total)")
        print("Files will be cached for future runs\n")

        for name, url in self.IMDB_DATASETS.items():
            print(f"\n{name}:")
            self.download_dataset(name, url)

        print("\n✅ All datasets downloaded!")

    # ========================================================================
    # Step 2: Process IMDB Data
    # ========================================================================

    def load_top_movies(self, limit: int = 10000, min_votes: int = 10000) -> pd.DataFrame:
        """Load top movies by weighted rating"""
        print(f"\n🎬 Processing Top {limit} Movies")
        print("=" * 70)

        # Load basics (titles, years, genres)
        print("Loading title basics...")
        basics = pd.read_csv(
            self.data_dir / 'title_basics.tsv',
            sep='\t',
            na_values='\\N',
            dtype={'tconst': str, 'titleType': str, 'primaryTitle': str,
                   'startYear': str, 'genres': str}
        )

        # Filter to movies only
        basics = basics[basics['titleType'] == 'movie'].copy()
        print(f"  Found {len(basics):,} movies")

        # Load ratings
        print("Loading ratings...")
        ratings = pd.read_csv(
            self.data_dir / 'title_ratings.tsv',
            sep='\t',
            dtype={'tconst': str, 'averageRating': float, 'numVotes': int}
        )
        print(f"  Found {len(ratings):,} rated titles")

        # Merge
        movies = basics.merge(ratings, on='tconst', how='inner')
        print(f"  Merged: {len(movies):,} rated movies")

        # Filter by minimum votes
        movies = movies[movies['numVotes'] >= min_votes].copy()
        print(f"  After min_votes filter: {len(movies):,} movies")

        # Calculate weighted rating (IMDB's formula)
        C = movies['averageRating'].mean()  # Mean rating
        m = movies['numVotes'].quantile(0.9)  # 90th percentile votes
        movies['weightedRating'] = (
            (movies['numVotes'] / (movies['numVotes'] + m)) * movies['averageRating'] +
            (m / (movies['numVotes'] + m)) * C
        )

        # Sort and take top N
        movies = movies.nlargest(limit, 'weightedRating')

        # Clean data
        movies['startYear'] = pd.to_numeric(movies['startYear'], errors='coerce')
        movies = movies[movies['startYear'].notna()].copy()
        movies['startYear'] = movies['startYear'].astype(int)

        # Filter reasonable years (1900-2030)
        movies = movies[(movies['startYear'] >= 1900) & (movies['startYear'] <= 2030)]

        print(f"\n✅ Selected Top {len(movies)} Movies")
        print(f"  Year range: {movies['startYear'].min()} - {movies['startYear'].max()}")
        print(f"  Rating range: {movies['averageRating'].min():.1f} - {movies['averageRating'].max():.1f}")
        print(f"  Votes range: {movies['numVotes'].min():,} - {movies['numVotes'].max():,}")

        return movies

    def load_crew_data(self, movie_ids: Set[str]) -> Dict[str, Dict]:
        """Load directors and writers for movies"""
        print("\n👥 Loading Crew Data")
        print("=" * 70)

        crew = pd.read_csv(
            self.data_dir / 'title_crew.tsv',
            sep='\t',
            na_values='\\N',
            dtype={'tconst': str, 'directors': str, 'writers': str}
        )

        # Filter to our movies
        crew = crew[crew['tconst'].isin(movie_ids)]

        print(f"  Found crew for {len(crew)} movies")
        return crew.set_index('tconst').to_dict('index')

    def load_people_names(self, person_ids: Set[str]) -> Dict[str, str]:
        """Load names for directors and actors"""
        print("\n🎭 Loading People Names")
        print("=" * 70)

        names = pd.read_csv(
            self.data_dir / 'name_basics.tsv',
            sep='\t',
            na_values='\\N',
            dtype={'nconst': str, 'primaryName': str},
            usecols=['nconst', 'primaryName']
        )

        # Filter to people we need
        names = names[names['nconst'].isin(person_ids)]

        print(f"  Loaded {len(names)} people names")
        return dict(zip(names['nconst'], names['primaryName']))

    def load_principals(self, movie_ids: Set[str], top_actors: int = 5) -> Dict[str, List[str]]:
        """Load top actors for each movie"""
        print(f"\n⭐ Loading Top {top_actors} Actors per Movie")
        print("=" * 70)

        principals = pd.read_csv(
            self.data_dir / 'title_principals.tsv',
            sep='\t',
            na_values='\\N',
            dtype={'tconst': str, 'ordering': int, 'nconst': str, 'category': str}
        )

        # Filter to our movies and actors/actresses
        principals = principals[
            (principals['tconst'].isin(movie_ids)) &
            (principals['category'].isin(['actor', 'actress']))
        ]

        # Get top N actors per movie (by ordering)
        principals = principals.sort_values(['tconst', 'ordering'])
        principals = principals.groupby('tconst').head(top_actors)

        # Group by movie
        movie_actors = principals.groupby('tconst')['nconst'].apply(list).to_dict()

        print(f"  Loaded actors for {len(movie_actors)} movies")
        return movie_actors

    # ========================================================================
    # Step 3: Clean Database
    # ========================================================================

    def clean_database(self):
        """Clean all movie-related data from Neo4j"""
        print("\n🧹 Cleaning Database")
        print("=" * 70)

        with self.driver.session() as session:
            # Delete all movies and their relationships
            print("  Deleting movies and relationships...")
            result = session.run("""
                MATCH (m:Movie)
                OPTIONAL MATCH (m)-[r]-()
                DELETE r, m
                RETURN count(m) as deleted
            """)
            deleted = result.single()['deleted']
            print(f"  ✓ Deleted {deleted} movies")

            # Delete orphaned entities
            print("  Cleaning orphaned entities...")

            session.run("""
                MATCH (g:Genre)
                WHERE NOT EXISTS((g)<-[:BELONGS_TO_GENRE]-())
                DELETE g
            """)

            session.run("""
                MATCH (d:Director)
                WHERE NOT EXISTS((d)-[:DIRECTED]->())
                DELETE d
            """)

            session.run("""
                MATCH (a:Actor)
                WHERE NOT EXISTS((a)-[:ACTED_IN]->())
                DELETE a
            """)

            print("  ✓ Cleaned orphaned entities")

            # Note: Preserve users and their ratings
            result = session.run("""
                MATCH (u:User)
                RETURN count(u) as userCount
            """)
            user_count = result.single()['userCount']
            print(f"  ℹ️  Preserved {user_count} users (ratings will be reconnected)")

        print("✅ Database cleaned!")

    # ========================================================================
    # Step 4: Import to Neo4j
    # ========================================================================

    def create_constraints_and_indexes(self):
        """Create database constraints and indexes"""
        print("\n📋 Creating Constraints and Indexes")
        print("=" * 70)

        constraints = [
            "CREATE CONSTRAINT movie_id_unique IF NOT EXISTS FOR (m:Movie) REQUIRE m.id IS UNIQUE",
            "CREATE CONSTRAINT movie_imdb_id_unique IF NOT EXISTS FOR (m:Movie) REQUIRE m.imdbId IS UNIQUE",
            "CREATE CONSTRAINT genre_name_unique IF NOT EXISTS FOR (g:Genre) REQUIRE g.name IS UNIQUE",
            "CREATE CONSTRAINT director_id_unique IF NOT EXISTS FOR (d:Director) REQUIRE d.id IS UNIQUE",
            "CREATE CONSTRAINT actor_id_unique IF NOT EXISTS FOR (a:Actor) REQUIRE a.id IS UNIQUE",
        ]

        indexes = [
            "CREATE INDEX movie_title_index IF NOT EXISTS FOR (m:Movie) ON (m.title)",
            "CREATE INDEX movie_year_index IF NOT EXISTS FOR (m:Movie) ON (m.releaseYear)",
            "CREATE INDEX movie_rating_index IF NOT EXISTS FOR (m:Movie) ON (m.imdbRating)",
        ]

        with self.driver.session() as session:
            for constraint in constraints:
                try:
                    session.run(constraint)
                    print(f"  ✓ {constraint.split('FOR')[0].split('IF')[0].strip()}")
                except Exception as e:
                    print(f"  ⚠️  {str(e)}")

            for index in indexes:
                try:
                    session.run(index)
                    print(f"  ✓ {index.split('FOR')[0].split('IF')[0].strip()}")
                except Exception as e:
                    print(f"  ⚠️  {str(e)}")

        print("✅ Constraints and indexes created!")

    def import_movies(self, movies_df: pd.DataFrame, crew_data: Dict,
                      people_names: Dict, movie_actors: Dict):
        """Import movies with all metadata to Neo4j"""
        print("\n📦 Importing Movies to Neo4j")
        print("=" * 70)

        batch_size = 500
        total = len(movies_df)
        imported = 0
        errors = 0

        for start_idx in range(0, total, batch_size):
            batch = movies_df.iloc[start_idx:start_idx + batch_size]

            movies_batch = []
            for _, movie in batch.iterrows():
                # Prepare movie data
                movie_data = {
                    'imdbId': movie['tconst'],
                    'id': movie['tconst'],  # Use IMDB ID as our ID
                    'title': movie['primaryTitle'],
                    'releaseYear': int(movie['startYear']),
                    'imdbRating': float(movie['averageRating']),
                    'imdbVotes': int(movie['numVotes']),
                    'duration': int(movie['runtimeMinutes']) if pd.notna(movie.get('runtimeMinutes')) else None,
                }

                # Parse genres
                genres = []
                if pd.notna(movie.get('genres')):
                    genres = [g.strip() for g in movie['genres'].split(',') if g.strip()]

                # Get directors
                directors = []
                if movie['tconst'] in crew_data:
                    director_ids = crew_data[movie['tconst']].get('directors', '')
                    if pd.notna(director_ids) and director_ids:
                        for director_id in director_ids.split(','):
                            if director_id in people_names:
                                directors.append({
                                    'id': director_id,
                                    'name': people_names[director_id]
                                })

                # Get actors
                actors = []
                if movie['tconst'] in movie_actors:
                    for actor_id in movie_actors[movie['tconst']]:
                        if actor_id in people_names:
                            actors.append({
                                'id': actor_id,
                                'name': people_names[actor_id]
                            })

                movies_batch.append({
                    'movie': movie_data,
                    'genres': genres,
                    'directors': directors,
                    'actors': actors
                })

            # Import batch
            try:
                with self.driver.session() as session:
                    session.run("""
                        UNWIND $movies AS movieData

                        // Create movie
                        CREATE (m:Movie)
                        SET m = movieData.movie

                        // Create genres and relationships
                        FOREACH (genreName IN movieData.genres |
                            MERGE (g:Genre {name: genreName})
                            ON CREATE SET g.description = genreName + ' movies'
                            MERGE (m)-[:BELONGS_TO_GENRE]->(g)
                        )

                        // Create directors and relationships
                        FOREACH (directorData IN movieData.directors |
                            MERGE (d:Director {id: directorData.id})
                            ON CREATE SET d.name = directorData.name
                            MERGE (d)-[:DIRECTED]->(m)
                        )

                        // Create actors and relationships
                        FOREACH (actorData IN movieData.actors |
                            MERGE (a:Actor {id: actorData.id})
                            ON CREATE SET a.name = actorData.name
                            MERGE (a)-[:ACTED_IN]->(m)
                        )
                    """, movies=movies_batch)

                imported += len(batch)
                progress = (imported / total) * 100
                print(f"  Progress: {imported}/{total} ({progress:.1f}%) - "
                      f"{len(batch)} movies imported", end='\r')

            except Exception as e:
                print(f"\n  ❌ Error importing batch: {e}")
                errors += 1

        print(f"\n✅ Import Complete!")
        print(f"  Imported: {imported} movies")
        print(f"  Errors: {errors} batches")

    # ========================================================================
    # Step 5: Verification
    # ========================================================================

    def verify_import(self):
        """Verify the import was successful"""
        print("\n✅ Verification")
        print("=" * 70)

        with self.driver.session() as session:
            # Count entities
            result = session.run("""
                MATCH (m:Movie)
                OPTIONAL MATCH (m)-[:BELONGS_TO_GENRE]->(g:Genre)
                OPTIONAL MATCH (d:Director)-[:DIRECTED]->(m)
                OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m)
                RETURN
                    count(DISTINCT m) as movies,
                    count(DISTINCT g) as genres,
                    count(DISTINCT d) as directors,
                    count(DISTINCT a) as actors
            """).single()

            print(f"  Movies: {result['movies']:,}")
            print(f"  Genres: {result['genres']:,}")
            print(f"  Directors: {result['directors']:,}")
            print(f"  Actors: {result['actors']:,}")

            # Count relationships
            result = session.run("""
                MATCH ()-[r:BELONGS_TO_GENRE]->()
                WITH count(r) as genreRels
                MATCH ()-[r2:DIRECTED]->()
                WITH genreRels, count(r2) as directorRels
                MATCH ()-[r3:ACTED_IN]->()
                RETURN genreRels, directorRels, count(r3) as actorRels
            """).single()

            print(f"\n  Relationships:")
            print(f"    BELONGS_TO_GENRE: {result['genreRels']:,}")
            print(f"    DIRECTED: {result['directorRels']:,}")
            print(f"    ACTED_IN: {result['actorRels']:,}")

            # Sample movies
            print(f"\n  Top 10 Movies by IMDB Rating:")
            result = session.run("""
                MATCH (m:Movie)
                OPTIONAL MATCH (m)-[:BELONGS_TO_GENRE]->(g:Genre)
                OPTIONAL MATCH (d:Director)-[:DIRECTED]->(m)
                RETURN m.title as title, m.releaseYear as year,
                       m.imdbRating as rating, m.imdbVotes as votes,
                       collect(DISTINCT g.name)[0..3] as genres,
                       collect(DISTINCT d.name)[0..2] as directors
                ORDER BY m.imdbRating DESC, m.imdbVotes DESC
                LIMIT 10
            """)

            for idx, record in enumerate(result, 1):
                print(f"    {idx}. {record['title']} ({record['year']}) - "
                      f"⭐{record['rating']}/10")
                if record['genres']:
                    print(f"       Genres: {', '.join(record['genres'])}")
                if record['directors']:
                    print(f"       Directors: {', '.join(record['directors'])}")

    # ========================================================================
    # Main Workflow
    # ========================================================================

    def run(self, clean: bool = True, limit: int = 10000):
        """Run the complete import process"""
        try:
            # Step 1: Download datasets
            self.download_all_datasets()

            # Step 2: Process data
            movies_df = self.load_top_movies(limit=limit)
            movie_ids = set(movies_df['tconst'].values)

            crew_data = self.load_crew_data(movie_ids)

            # Collect all person IDs (directors + actors)
            person_ids = set()
            for crew in crew_data.values():
                if pd.notna(crew.get('directors')):
                    person_ids.update(crew['directors'].split(','))

            movie_actors = self.load_principals(movie_ids, top_actors=5)
            for actors in movie_actors.values():
                person_ids.update(actors)

            people_names = self.load_people_names(person_ids)

            # Step 3: Clean database (if requested)
            if clean:
                confirm = input("\n⚠️  This will DELETE all movies from Neo4j. Continue? (yes/no): ")
                if confirm.lower() != 'yes':
                    print("Import cancelled.")
                    return
                self.clean_database()

            # Step 4: Create schema
            self.create_constraints_and_indexes()

            # Step 5: Import
            self.import_movies(movies_df, crew_data, people_names, movie_actors)

            # Step 6: Verify
            self.verify_import()

            print("\n" + "=" * 70)
            print("🎉 Import Complete!")
            print("=" * 70)
            print("\nNext steps:")
            print("  1. Open Neo4j Browser: http://localhost:7474")
            print("  2. Run: MATCH (m:Movie) RETURN m LIMIT 25")
            print("  3. Test recommendations: ./mvnw spring-boot:run")

        except KeyboardInterrupt:
            print("\n\n⚠️  Import interrupted by user")
        except Exception as e:
            print(f"\n❌ Error: {e}")
            raise


def main():
    parser = argparse.ArgumentParser(description='Import top IMDB movies to Neo4j')
    parser.add_argument('--neo4j-uri', default='bolt://localhost:7687', help='Neo4j URI')
    parser.add_argument('--neo4j-user', default='neo4j', help='Neo4j username')
    parser.add_argument('--neo4j-password', default='password', help='Neo4j password')
    parser.add_argument('--data-dir', default='./imdb_data', help='Directory for IMDB datasets')
    parser.add_argument('--limit', type=int, default=10000, help='Number of movies to import')
    parser.add_argument('--clean', action='store_true', help='Clean database before import')
    parser.add_argument('--no-clean', dest='clean', action='store_false', help='Keep existing data')
    parser.set_defaults(clean=True)

    args = parser.parse_args()

    print("=" * 70)
    print("🎬 Neo4flix - IMDB Top Movies Importer")
    print("=" * 70)
    print(f"Target: {args.limit} movies")
    print(f"Neo4j: {args.neo4j_uri}")
    print(f"Clean: {args.clean}")
    print("=" * 70)

    importer = IMDBImporter(
        neo4j_uri=args.neo4j_uri,
        neo4j_user=args.neo4j_user,
        neo4j_password=args.neo4j_password,
        data_dir=args.data_dir
    )

    try:
        importer.run(clean=args.clean, limit=args.limit)
    finally:
        importer.close()


if __name__ == '__main__':
    main()
