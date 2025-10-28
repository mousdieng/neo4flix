export const environment = {
  production: true,
  // Production API URL - should use HTTPS
  apiUrl: 'https://api.neo4flix.com/api/v1',
  gatewayUrl: 'https://api.neo4flix.com',

  // Service endpoints (through gateway)
  endpoints: {
    auth: '/auth',
    users: '/users',
    movies: '/movies',
    ratings: '/ratings',
    recommendations: '/recommendations',
    watchlist: '/watchlist'
  },

  // App configuration
  app: {
    name: 'Neo4flix',
    version: '1.0.0'
  },

  // Feature flags
  features: {
    enableRecommendations: true,
    enableWatchlist: true,
    enableSocialFeatures: true
  }
};
