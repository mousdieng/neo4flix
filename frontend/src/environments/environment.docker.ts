export const environment = {
  production: true,
  // Docker environment - frontend and gateway both in same Docker network
  apiUrl: 'http://gateway-service:9080/api/v1',
  gatewayUrl: 'http://gateway-service:9080',

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
