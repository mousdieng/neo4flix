# 🎬 Neo4flix Frontend

A modern movie discovery and rating platform built with Angular 20, featuring Netflix-inspired design and advanced recommendation algorithms.

## 🚀 Features

### 🔐 Authentication System
- **User Registration & Login** with JWT token management
- **Protected Routes** with authentication guards
- **Persistent Sessions** with local storage
- **Password Visibility Toggle** for better UX

### 🎥 Movie Discovery
- **Advanced Search & Filtering** by title, genre, year, rating
- **Real-time Search** with debounced input
- **Pagination** for large datasets
- **Genre-based Browsing** with clickable tags
- **Responsive Movie Cards** with poster images and ratings

### ⭐ Rating & Review System
- **Interactive Star Ratings** (5-star display, 10-point backend)
- **Detailed Reviews** with character limits
- **User Rating Management** (create, edit, delete)
- **Review Display** with user information and timestamps

### 🤖 AI-Powered Recommendations
- **Personalized Recommendations** based on user preferences
- **Content-based Filtering** for similar movies
- **Collaborative Filtering** using user behavior
- **Trending Movies** and popularity-based suggestions
- **User Interaction Tracking** for improved recommendations

### 🎨 Modern UI/UX
- **Netflix-inspired Design** with dark theme
- **TailwindCSS Styling** for consistent design system
- **Responsive Layout** optimized for all devices
- **Loading States** and error handling
- **Smooth Animations** and transitions

## 🏗️ Technical Architecture

### Frontend Stack
- **Angular 20** with Standalone Components
- **TypeScript** for type safety
- **RxJS** for reactive programming
- **Angular Signals** for state management
- **TailwindCSS** for styling
- **Vite** for fast development builds

### Project Structure
```
src/
├── app/
│   ├── components/         # Reusable UI components
│   │   ├── rating/        # Star rating component
│   │   └── review/        # Review form and display
│   ├── pages/             # Route components
│   │   ├── home/          # Landing page with recommendations
│   │   ├── movies/        # Movie browsing and search
│   │   └── auth/          # Login and registration
│   ├── services/          # Business logic and API calls
│   │   ├── auth.ts        # Authentication service
│   │   ├── movie.ts       # Movie data service
│   │   ├── rating.ts      # Rating and review service
│   │   └── recommendation.ts # AI recommendation service
│   └── app.routes.ts      # Application routing
└── styles.css             # Global styles and Tailwind imports
```

### Key Services

#### Authentication Service (`auth.ts`)
- JWT token management
- User session persistence
- Login/logout functionality
- Authentication state management

#### Movie Service (`movie.ts`)
- Movie search and filtering
- Pagination support
- Genre management
- Popular/recent/top-rated movie lists

#### Rating Service (`rating.ts`)
- Submit and manage ratings/reviews
- User rating history
- Movie rating statistics
- Review CRUD operations

#### Recommendation Service (`recommendation.ts`)
- Personalized recommendations
- Content-based filtering
- Trending movie suggestions
- User interaction tracking

## 🛠️ Development Setup

### Prerequisites
- Node.js 18+ and npm
- Angular CLI 20+

### Installation
```bash
# Install dependencies
npm install

# Start development server
npm start

# Run tests
npm test

# Build for production
npm run build
```

### Development Server
- **URL**: http://localhost:4200/
- **Hot Reload**: Enabled
- **Source Maps**: Available in development

## 📦 Build Configuration

### Development Build
- **Bundle Size**: ~145 KB (uncompressed)
- **Hot Module Replacement**: Enabled
- **Source Maps**: Included

### Production Build
- **Bundle Size**: 99.60 KB (gzipped)
- **Tree Shaking**: Enabled
- **Code Splitting**: Automatic
- **Output**: `dist/frontend/`

## 🧪 Testing

### Test Framework
- **Jasmine**: Testing framework
- **Karma**: Test runner
- **Angular Testing Utilities**: Component testing

### Test Coverage
- Component creation tests
- Service integration tests
- UI interaction tests

## 🌐 API Integration

The frontend is designed to work with the Neo4flix microservices backend:

### Gateway Service
- **Base URL**: `http://localhost:8080`
- **Authentication**: JWT tokens in headers
- **Route Prefix**: `/api/v1/`

### Service Endpoints
- **Auth**: `/api/v1/auth/*`
- **Movies**: `/api/v1/movies/*`
- **Ratings**: `/api/v1/ratings/*`
- **Recommendations**: `/api/v1/recommendations/*`

## 🔧 Configuration

### Environment Files
- `src/environments/environment.ts` - Development config
- `src/environments/environment.prod.ts` - Production config

### TailwindCSS Configuration
- Custom Netflix color palette
- Responsive breakpoints
- Component classes for consistency

## 🚀 Deployment

### Production Build
```bash
npm run build
```

### Deployment Options
- **Static Hosting**: Netlify, Vercel, GitHub Pages
- **CDN**: CloudFront, CloudFlare
- **Container**: Docker with Nginx

### Build Optimization
- **Bundle Analysis**: Use `ng build --stats-json`
- **Performance**: Lazy loading routes
- **Caching**: Service worker ready

## 🔮 Future Enhancements

### Features Roadmap
- **Watchlist Management** - Save movies for later
- **Social Features** - Follow users, share reviews
- **Video Player** - Stream movie trailers
- **Offline Support** - PWA capabilities
- **Advanced Analytics** - User behavior insights

### Technical Improvements
- **Unit Test Coverage** - Increase to 90%+
- **E2E Testing** - Cypress integration
- **Performance Monitoring** - Real user metrics
- **Accessibility** - WCAG 2.1 compliance

## 📱 Responsive Design

### Breakpoints
- **Mobile**: 640px and below
- **Tablet**: 641px - 1024px
- **Desktop**: 1025px and above

### Mobile Optimizations
- Touch-friendly interface
- Optimized image loading
- Reduced motion for performance
- Thumb-accessible navigation

## 🎯 Performance Metrics

### Core Web Vitals
- **First Contentful Paint**: < 1.5s
- **Largest Contentful Paint**: < 2.5s
- **Cumulative Layout Shift**: < 0.1

### Bundle Analysis
- **Main Bundle**: 83.64 KB (gzipped)
- **Polyfills**: 11.33 KB (gzipped)
- **Styles**: 4.63 KB (gzipped)
- **Total**: 99.60 KB (gzipped)

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📄 License

This project is licensed under the MIT License.

---

**Built with ❤️ using Angular 20 and TailwindCSS**
