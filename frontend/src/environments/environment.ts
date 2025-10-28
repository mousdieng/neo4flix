export const environment = {
  production: false,
  // Use HTTPS in development (with self-signed cert) or HTTP
  // Change to https://localhost:9080 when using Gateway SSL or https://localhost when using Nginx
  apiUrl: 'https://localhost:9080/api/v1',
  gatewayUrl: 'https://localhost:9080',

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
