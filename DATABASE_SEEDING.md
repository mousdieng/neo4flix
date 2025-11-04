# Database Seeding Guide

This guide explains how to export and automatically load movie data in Neo4flix without using external APIs.

## Overview

The database seeding system allows you to:
1. **Export** all movie data from an existing Neo4j database to a JSON file
2. **Auto-load** that data when starting the application in a new environment
3. Run the application without requiring IMDB API calls or manual data import

## How It Works

### 1. Export Movie Data (One-Time Setup)

First, export your current movie data to a JSON file:

```bash
# Navigate to scripts directory
cd /home/moussa/dev/neo4flix/scripts

# Run export script
python3 export_movies.py \
  --neo4j-uri bolt://localhost:7687 \
  --neo4j-user neo4j \
  --neo4j-password password \
  --output movies_seed.json
```

**Options:**
- `--neo4j-uri`: Neo4j connection URI (default: bolt://localhost:7687)
- `--neo4j-user`: Neo4j username (default: neo4j)
- `--neo4j-password`: Neo4j password (default: password)
- `--output`: Output JSON file name (default: movies_seed.json)

**Output:**
```
======================================================================
🎬 Neo4flix - Movie Data Exporter
======================================================================
Neo4j: bolt://localhost:7687
Output: movies_seed.json
======================================================================
📤 Exporting movies from Neo4j...
  ✓ Found 10000 movies
  ✓ Found 24 genres
  ✓ Writing to movies_seed.json...
  ✓ Exported successfully (15.32 MB)

✅ Export complete!
   File: movies_seed.json
   Movies: 10000
   Genres: 24
```

### 2. Place Seed File in Resources

Move the exported JSON file to the recommendation service resources:

```bash
# Copy seed file to resources directory
cp movies_seed.json \
   /home/moussa/dev/neo4flix/microservices/recommendation-service/src/main/resources/
```

### 3. Automatic Loading on Startup

The application will automatically load the seed data when:
- The database is empty (no Movie nodes exist)
- Auto-seed is enabled (default: true)
- The seed file exists in resources

**Configuration** (application.properties):
```properties
# Enable/disable auto-seeding (default: true)
neo4flix.data.auto-seed=true

# Seed file name (default: movies_seed.json)
neo4flix.data.seed-file=movies_seed.json
```

## Usage Scenarios

### Scenario 1: Fresh Development Environment

```bash
# 1. Clone repository
git clone <repo-url>
cd neo4flix

# 2. Ensure seed file exists in resources
ls microservices/recommendation-service/src/main/resources/movies_seed.json

# 3. Start Neo4j (empty database)
docker-compose up -d neo4j

# 4. Start recommendation service
cd microservices/recommendation-service
./mvnw spring-boot:run

# ✅ Movies automatically loaded from seed file!
```

**Console Output:**
```
Checking if database needs initialization...
Database is empty. Starting initialization from seed file: movies_seed.json
Loading seed data from: movies_seed.json
Seed file contains 10000 movies
Creating 24 genres...
✓ Genres created successfully
Processed 100 movies...
Processed 200 movies...
...
Processed 10000 movies total
Successfully loaded 10000 movies from seed file
Database initialization completed successfully!
```

### Scenario 2: Production Deployment

```bash
# 1. Build application with seed file included
./mvnw clean package

# 2. Deploy and start services
docker-compose up -d

# ✅ First startup automatically loads movies
# ✅ Subsequent restarts skip loading (database not empty)
```

### Scenario 3: Update Seed Data

```bash
# 1. Export latest data
cd scripts
python3 export_movies.py --output movies_seed_v2.json

# 2. Replace old seed file
cp movies_seed_v2.json \
   ../microservices/recommendation-service/src/main/resources/movies_seed.json

# 3. Clean database and restart
docker-compose down -v
docker-compose up -d

# ✅ New seed data loaded automatically
```

### Scenario 4: Disable Auto-Seeding

If you want to manually import data or use external APIs:

```properties
# application.properties
neo4flix.data.auto-seed=false
```

Or via environment variable:
```bash
export NEO4FLIX_DATA_AUTO_SEED=false
./mvnw spring-boot:run
```

## Seed File Format

The JSON seed file has this structure:

```json
{
  "version": "1.0",
  "exported_at": "2024-10-30T14:30:00Z",
  "total_movies": 10000,
  "total_genres": 24,
  "genres": [
    "Action",
    "Comedy",
    "Drama",
    ...
  ],
  "movies": [
    {
      "id": "movie-123",
      "title": "The Shawshank Redemption",
      "plot": "Two imprisoned men bond over...",
      "releaseYear": 1994,
      "runtime": 142,
      "imdbRating": 9.3,
      "imdbVotes": 2500000,
      "posterUrl": "https://...",
      "backdropUrl": "https://...",
      "genres": ["Drama"],
      "directors": [
        {"id": "dir-456", "name": "Frank Darabont"}
      ],
      "actors": [
        {"id": "act-789", "name": "Tim Robbins"},
        {"id": "act-790", "name": "Morgan Freeman"}
      ]
    },
    ...
  ]
}
```

## Data Import Details

### What Gets Imported

✅ **Movies**: All movie properties (title, plot, year, rating, etc.)
✅ **Genres**: All genre nodes
✅ **Directors**: Director nodes with DIRECTED relationships
✅ **Actors**: Actor nodes with ACTED_IN relationships
✅ **Relationships**: All BELONGS_TO_GENRE, DIRECTED, ACTED_IN relationships

### What Doesn't Get Imported

❌ **User Data**: Users, ratings, watchlists, friend requests
❌ **Recommendations**: User-specific recommendations
❌ **Shared Movies**: Shared recommendations between users
❌ **Similarity Data**: SIMILAR_TO relationships (computed by GDS)

### Performance

- **Export Time**: ~30-60 seconds for 10,000 movies
- **Import Time**: ~2-3 minutes for 10,000 movies (100 movies per batch)
- **File Size**: ~15-20 MB for 10,000 movies with full metadata

### Batch Processing

Movies are imported in batches of 100 to optimize performance and memory usage:

```
Processed 100 movies...
Processed 200 movies...
Processed 300 movies...
...
Processed 10000 movies total
```

## Troubleshooting

### Issue: "Seed file not found"

**Cause**: movies_seed.json not in resources directory

**Solution**:
```bash
ls microservices/recommendation-service/src/main/resources/movies_seed.json
# If missing, copy it there
cp scripts/movies_seed.json \
   microservices/recommendation-service/src/main/resources/
```

### Issue: Movies not loading

**Cause**: Database already contains movies

**Solution**: Seeding only runs on empty databases. To reload:
```bash
# Option 1: Clear database
docker-compose down -v
docker-compose up -d

# Option 2: Clear via Cypher
docker exec -it neo4flix-neo4j cypher-shell \
  -u neo4j -p password \
  "MATCH (n) DETACH DELETE n"
```

### Issue: "Failed to initialize database"

**Cause**: Invalid JSON or connection issues

**Solution**: Check logs for specific error and verify:
- JSON file is valid
- Neo4j is running and accessible
- Connection credentials are correct

### Issue: Duplicate movies

**Cause**: Movies already exist with same IDs

**Solution**: The import uses MERGE, so duplicates shouldn't occur. If they do:
```bash
# Clean database and reimport
docker-compose down -v
docker-compose up -d
```

## Best Practices

### 1. Version Control
✅ **Do** commit the seed file to git if it's stable reference data
❌ **Don't** commit if it contains sensitive or frequently changing data

### 2. Seed File Updates
- Export fresh data periodically
- Update seed file when adding significant new movies
- Version your seed files (movies_seed_v1.json, movies_seed_v2.json)

### 3. Development vs Production
- **Development**: Keep auto-seed enabled for quick setup
- **Production**: Consider disabling after first deployment

### 4. Large Datasets
For very large datasets (100k+ movies):
- Increase batch size in DatabaseInitializer
- Consider compressing seed file (gzip)
- Use database backups instead of JSON seeding

## Integration with Existing Import Script

You can still use the IMDB import script for initial data:

```bash
# Option 1: Use IMDB import script
cd scripts
python3 import_imdb_top_10000.py --clean --limit 10000

# Then export for future use
python3 export_movies.py --output movies_seed.json

# Option 2: Use seed file (faster, no API calls)
# Just start the application with seed file in resources
```

## Summary

**To enable database seeding:**
1. Export current data: `python3 export_movies.py`
2. Copy to resources: `cp movies_seed.json microservices/.../resources/`
3. Ensure auto-seed is enabled (default)
4. Start application on empty database

**Benefits:**
- ✅ No external API dependencies
- ✅ Fast startup in new environments
- ✅ Consistent data across environments
- ✅ No manual import steps
- ✅ Automatic on first run
