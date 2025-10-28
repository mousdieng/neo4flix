// Neo4flix Database Diagnostic Script
// Run these queries in Neo4j Browser to check your database state

// 1. Check total counts
MATCH (m:Movie) RETURN count(m) AS totalMovies;
MATCH (u:User) RETURN count(u) AS totalUsers;
MATCH (g:Genre) RETURN count(g) AS totalGenres;
MATCH (d:Director) RETURN count(d) AS totalDirectors;
MATCH ()-[r:RATED]->() RETURN count(r) AS totalRatings;

// 2. Check relationship types (to verify what exists)
CALL db.relationshipTypes() YIELD relationshipType RETURN relationshipType;

// 3. Check if movies have genres
MATCH (m:Movie)-[r:BELONGS_TO_GENRE]->(g:Genre)
RETURN count(DISTINCT m) AS moviesWithGenres,
       count(r) AS totalGenreRelationships;

// 4. Check if movies have directors
MATCH (m:Movie)<-[r:DIRECTED]-(d:Director)
RETURN count(DISTINCT m) AS moviesWithDirectors,
       count(r) AS totalDirectorRelationships;

// 5. Check ratings distribution
MATCH (u:User)-[r:RATED]->(m:Movie)
RETURN count(DISTINCT u) AS usersWhoRated,
       count(DISTINCT m) AS moviesRated,
       count(r) AS totalRatings,
       avg(r.rating) AS avgRating;

// 6. Check your specific user's ratings
MATCH (u:User {id: '3d519f62-6c26-409a-bd3d-3cf978ccf5d4'})-[r:RATED]->(m:Movie)
RETURN count(r) AS yourRatings,
       avg(r.rating) AS yourAvgRating;

// 7. Check if there are other users with ratings (needed for collaborative filtering)
MATCH (u:User)-[r:RATED]->()
WHERE u.id <> '3d519f62-6c26-409a-bd3d-3cf978ccf5d4'
RETURN count(DISTINCT u) AS otherUsersWithRatings,
       count(r) AS theirTotalRatings;

// 8. Sample movies without genres (if any)
MATCH (m:Movie)
WHERE NOT EXISTS((m)-[:BELONGS_TO_GENRE]->(:Genre))
RETURN m.title, m.releaseYear
LIMIT 5;

// 9. Sample movies without directors (if any)
MATCH (m:Movie)
WHERE NOT EXISTS((m)<-[:DIRECTED]-(:Director))
RETURN m.title, m.releaseYear
LIMIT 5;

// 10. Check movie properties
MATCH (m:Movie)
RETURN m.title, m.imdbRating,
       EXISTS((m)-[:BELONGS_TO_GENRE]->()) AS hasGenres,
       EXISTS((m)<-[:DIRECTED]-()) AS hasDirector,
       size((m)<-[:RATED]-()) AS ratingCount
LIMIT 10;
