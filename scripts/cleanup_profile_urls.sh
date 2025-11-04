#!/bin/bash
# ========================================================================
# Cleanup Profile Picture URLs Script
# ========================================================================
# This script converts presigned MinIO URLs to object keys in the database
#
# Usage: ./cleanup_profile_urls.sh
# ========================================================================

echo "========================================================================"
echo "🔧 Neo4flix - Profile Picture URL Cleanup"
echo "========================================================================"
echo ""

# Check if docker is running
if ! docker ps | grep -q neo4flix-neo4j; then
    echo "❌ Error: Neo4j container is not running"
    echo "   Start it with: docker-compose up -d neo4j"
    exit 1
fi

echo "📊 Step 1: Checking how many users need cleanup..."
COUNT=$(docker exec neo4flix-neo4j cypher-shell -u neo4j -p password \
    "MATCH (u:User) WHERE u.profilePictureUrl STARTS WITH 'http' RETURN count(u) AS count" \
    --format plain | grep -E '^[0-9]+$' | head -1)

if [ -z "$COUNT" ] || [ "$COUNT" = "0" ]; then
    echo "✅ No users need cleanup. All profile picture URLs are already object keys!"
    exit 0
fi

echo "   Found $COUNT users with presigned URLs"
echo ""

echo "📝 Step 2: Preview transformation (first 5 users)..."
docker exec neo4flix-neo4j cypher-shell -u neo4j -p password "
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
WITH u, u.profilePictureUrl AS oldUrl
WITH u, oldUrl,
     split(split(oldUrl, '?')[0], '/')[4..] AS pathParts
WITH u, oldUrl,
     reduce(s = '', part IN pathParts | s + CASE WHEN s = '' THEN part ELSE '/' + part END) AS newKey
RETURN u.username, oldUrl, newKey
LIMIT 5
" --format plain

echo ""
read -p "🤔 Do you want to proceed with cleanup? (y/n) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Cleanup cancelled"
    exit 0
fi

echo ""
echo "🔄 Step 3: Cleaning up profile picture URLs..."
UPDATED=$(docker exec neo4flix-neo4j cypher-shell -u neo4j -p password "
MATCH (u:User)
WHERE u.profilePictureUrl STARTS WITH 'http'
WITH u, u.profilePictureUrl AS oldUrl
WITH u, oldUrl,
     split(split(oldUrl, '?')[0], '/')[4..] AS pathParts
WITH u, oldUrl,
     reduce(s = '', part IN pathParts | s + CASE WHEN s = '' THEN part ELSE '/' + part END) AS objectKey
SET u.profilePictureUrl = objectKey
RETURN count(u) AS count
" --format plain | grep -E '^[0-9]+$' | head -1)

echo "   ✓ Updated $UPDATED users"
echo ""

echo "✅ Step 4: Verifying cleanup..."
REMAINING=$(docker exec neo4flix-neo4j cypher-shell -u neo4j -p password \
    "MATCH (u:User) WHERE u.profilePictureUrl STARTS WITH 'http' RETURN count(u) AS count" \
    --format plain | grep -E '^[0-9]+$' | head -1)

if [ "$REMAINING" = "0" ]; then
    echo "   ✓ All presigned URLs have been converted to object keys!"
else
    echo "   ⚠️  Warning: $REMAINING users still have presigned URLs"
fi

echo ""
echo "📋 Sample of cleaned data:"
docker exec neo4flix-neo4j cypher-shell -u neo4j -p password "
MATCH (u:User)
WHERE u.profilePictureUrl IS NOT NULL
RETURN u.username, u.profilePictureUrl
LIMIT 5
" --format plain

echo ""
echo "========================================================================"
echo "✅ Cleanup complete!"
echo "========================================================================"
echo ""
echo "Next steps:"
echo "  1. Restart your microservices to apply the code changes"
echo "  2. Profile pictures will now use fresh 15-minute presigned URLs"
echo "  3. URLs are generated on-the-fly, never stored in database"
echo ""
