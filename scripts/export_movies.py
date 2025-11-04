#!/usr/bin/env python3
"""
Neo4flix - Export Movies to JSON
=================================
Exports all movies from Neo4j database to a JSON file that can be
loaded on application startup without external API calls.

Usage:
    python export_movies.py --output movies_seed.json
"""

import argparse
import json
from neo4j import GraphDatabase
from typing import List, Dict, Any


class MovieExporter:
    """Export movies from Neo4j to JSON"""

    def __init__(self, neo4j_uri: str, neo4j_user: str, neo4j_password: str):
        self.driver = GraphDatabase.driver(neo4j_uri, auth=(neo4j_user, neo4j_password))

    def close(self):
        self.driver.close()

    def export_movies(self) -> List[Dict[str, Any]]:
        """Export all movies with their genres, directors, and actors"""

        query = """
        MATCH (m:Movie)
        OPTIONAL MATCH (m)-[:BELONGS_TO_GENRE]->(g:Genre)
        OPTIONAL MATCH (d:Director)-[:DIRECTED]->(m)
        OPTIONAL MATCH (a:Actor)-[:ACTED_IN]->(m)
        WITH m,
             collect(DISTINCT g.name) as genres,
             collect(DISTINCT {id: d.id, name: d.name}) as directors,
             collect(DISTINCT {id: a.id, name: a.name}) as actors
        RETURN m.id as id,
               m.title as title,
               m.plot as plot,
               m.releaseYear as releaseYear,
               m.duration as runtime,
               m.imdbRating as imdbRating,
               m.imdbVotes as imdbVotes,
               m.posterUrl as posterUrl,
               m.backdropUrl as backdropUrl,
               genres,
               directors,
               actors
        ORDER BY m.title
        """

        with self.driver.session() as session:
            result = session.run(query)
            movies = []

            for record in result:
                movie = {
                    'id': record['id'],
                    'title': record['title'],
                    'plot': record['plot'],
                    'releaseYear': record['releaseYear'],
                    'runtime': record['runtime'],
                    'imdbRating': record['imdbRating'],
                    'imdbVotes': record['imdbVotes'],
                    'posterUrl': record['posterUrl'],
                    'backdropUrl': record['backdropUrl'],
                    'genres': [g for g in record['genres'] if g],  # Filter out nulls
                    'directors': [d for d in record['directors'] if d and d['id']],
                    'actors': [a for a in record['actors'] if a and a['id']]
                }
                movies.append(movie)

            return movies

    def export_genres(self) -> List[str]:
        """Export all unique genres"""
        query = "MATCH (g:Genre) RETURN g.name as name ORDER BY g.name"

        with self.driver.session() as session:
            result = session.run(query)
            return [record['name'] for record in result]

    def export_to_json(self, output_file: str):
        """Export movies and genres to JSON file"""
        print(f"📤 Exporting movies from Neo4j...")

        movies = self.export_movies()
        genres = self.export_genres()

        data = {
            'version': '1.0',
            'exported_at': None,  # Will be set by json serializer
            'total_movies': len(movies),
            'total_genres': len(genres),
            'genres': genres,
            'movies': movies
        }

        # Add timestamp
        from datetime import datetime
        data['exported_at'] = datetime.utcnow().isoformat() + 'Z'

        print(f"  ✓ Found {len(movies)} movies")
        print(f"  ✓ Found {len(genres)} genres")
        print(f"  ✓ Writing to {output_file}...")

        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=2, ensure_ascii=False)

        # Show file size
        import os
        size_mb = os.path.getsize(output_file) / (1024 * 1024)
        print(f"  ✓ Exported successfully ({size_mb:.2f} MB)")
        print(f"\n✅ Export complete!")
        print(f"   File: {output_file}")
        print(f"   Movies: {len(movies)}")
        print(f"   Genres: {len(genres)}")


def main():
    parser = argparse.ArgumentParser(description='Export Neo4j movies to JSON')
    parser.add_argument('--neo4j-uri', default='bolt://localhost:7687', help='Neo4j URI')
    parser.add_argument('--neo4j-user', default='neo4j', help='Neo4j username')
    parser.add_argument('--neo4j-password', default='password', help='Neo4j password')
    parser.add_argument('--output', default='movies_seed.json', help='Output JSON file')

    args = parser.parse_args()

    print("=" * 70)
    print("🎬 Neo4flix - Movie Data Exporter")
    print("=" * 70)
    print(f"Neo4j: {args.neo4j_uri}")
    print(f"Output: {args.output}")
    print("=" * 70)

    exporter = MovieExporter(
        neo4j_uri=args.neo4j_uri,
        neo4j_user=args.neo4j_user,
        neo4j_password=args.neo4j_password
    )

    try:
        exporter.export_to_json(args.output)
    finally:
        exporter.close()


if __name__ == '__main__':
    main()
