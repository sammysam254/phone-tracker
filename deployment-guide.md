# 🚀 Deployment Guide - Parental Control Platform

## 📋 **Pre-Deployment Checklist**

### ✅ **1. Supabase Setup**
- [ ] Run `supabase/schema-update.sql` in Supabase SQL Editor
- [ ] Create storage buckets: `monitoring-images`, `monitoring-audio`
- [ ] Configure Row Level Security policies
- [ ] Get Supabase URL and API keys

### ✅ **2. Environment Variables**
Required for all platforms:
```env
SUPABASE_URL=https://gejzprqznycnbfzeaxza.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
NODE_ENV=production
PORT=3000
```

---

## 🌐 **Platform Deployment Options**

### **1. 🟢 Render (Recommended)**

**Steps:**
1. Connect GitHub repository to Render
2. Create new Web Service
3. Use these settings:
   - **Build Command**: `npm install`
   - **Start Command**: `npm start`
   - **Environment**: Node.js
   - **Plan**: Starter ($7/month)

**Environment Variables in Render:**
```
SUPABASE_URL=https://gejzprqznycnbfzeaxza.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
NODE_ENV=production
```

**Auto-Deploy:** ✅ Enabled with `render.yaml`

---

### **2. ⚡ Vercel**

**Steps:**
1. Install Vercel CLI: `npm i -g vercel`
2. Run: `vercel --prod`
3. Configure environment variables in Vercel dashboard

**Features:**
- ✅ Automatic deployments
- ✅ Global CDN
- ✅ Serverless functions
- ✅ Free tier available

---

### **3. 🌊 Netlify**

**For Static Dashboard Only:**
1. Drag & drop `web-dashboard` folder to Netlify
2. Configure redirects with `netlify.toml`
3. Set environment variables for API endpoints

**Note:** Backend needs separate hosting (Render/Heroku)

---

### **4. 🐳 Docker Deployment**

**Local Docker:**
```bash
# Build and run
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

**Production Docker:**
```bash
# Build image
docker build -t parental-control .

# Run container
docker run -d -p 3000:3000 \
  -e SUPABASE_URL=your-url \
  -e SUPABASE_ANON_KEY=your-key \
  parental-control
```

---

### **5. ☁️ Railway**

**Steps:**
1. Connect GitHub to Railway
2. Deploy from repository
3. Add environment variables
4. Custom domain (optional)

**Features:**
- ✅ $5/month starter
- ✅ Automatic scaling
- ✅ Built-in monitoring

---

### **6. 🚀 DigitalOcean App Platform**

**Steps:**
1. Create new app from GitHub
2. Configure build settings
3. Add environment variables
4. Deploy

**Pricing:** $5-12/month

---

## 📱 **Web Dashboard Deployment**

### **Static Hosting Options:**

**1. Netlify (Free)**
- Drag & drop `web-dashboard` folder
- Automatic HTTPS
- Custom domains

**2. Vercel (Free)**
- GitHub integration
- Global CDN
- Instant deployments

**3. GitHub Pages (Free)**
- Push to `gh-pages` branch
- Custom domain support
- HTTPS included

**4. Firebase Hosting (Free)**
```bash
npm install -g firebase-tools
firebase init hosting
firebase deploy
```

---

## 🔧 **Configuration Updates**

### **Update API URLs:**

**In `web-dashboard/dashboard.js`:**
```javascript
// Replace with your deployed backend URL
const API_BASE_URL = 'https://your-app.render.com/api';
```

**In Android app `SupabaseClient.java`:**
```java
// Already configured with your Supabase URL
private static final String SUPABASE_URL = "https://gejzprqznycnbfzeaxza.supabase.co";
```

---

## 🛡️ **Security Configuration**

### **1. CORS Setup**
```javascript
// In backend/server.js
app.use(cors({
  origin: [
    'https://your-dashboard.netlify.app',
    'https://your-domain.com'
  ]
}));
```

### **2. Environment Security**
- ✅ Never commit `.env` files
- ✅ Use platform environment variables
- ✅ Rotate API keys regularly
- ✅ Enable HTTPS only

### **3. Supabase Security**
- ✅ Row Level Security enabled
- ✅ API key restrictions
- ✅ Storage bucket policies
- ✅ Rate limiting configured

---

## 📊 **Monitoring & Analytics**

### **Health Checks:**
- Backend: `https://your-api.com/health`
- Dashboard: Monitor uptime with UptimeRobot

### **Logging:**
- Render: Built-in logs
- Vercel: Function logs
- Docker: `docker logs container-name`

### **Performance:**
- Use Lighthouse for dashboard performance
- Monitor API response times
- Set up error tracking (Sentry)

---

## 🚀 **Quick Deploy Commands**

### **Render:**
```bash
# Push to GitHub, auto-deploys with render.yaml
git add .
git commit -m "Deploy to Render"
git push origin main
```

### **Vercel:**
```bash
npm i -g vercel
vercel --prod
```

### **Docker:**
```bash
docker-compose up -d
```

### **Netlify:**
```bash
# Install Netlify CLI
npm i -g netlify-cli

# Deploy dashboard
cd web-dashboard
netlify deploy --prod --dir .
```

---

## ✅ **Post-Deployment Testing**

### **1. Backend API:**
- [ ] Health check: `GET /health`
- [ ] User registration: `POST /api/register`
- [ ] Device pairing: `POST /api/device/register`
- [ ] Activity logging: `POST /api/activity`

### **2. Web Dashboard:**
- [ ] Parent registration works
- [ ] Device pairing with 6-digit code
- [ ] Real-time activity display
- [ ] Remote camera/audio commands

### **3. Android App:**
- [ ] Supabase connection
- [ ] Permission setup flow
- [ ] Device pairing process
- [ ] Activity monitoring active
- [ ] Remote commands received

---

## 🎯 **Recommended Deployment Stack**

**For Production:**
- **Backend**: Render Web Service ($7/month)
- **Dashboard**: Netlify (Free)
- **Database**: Supabase (Free tier)
- **Domain**: Custom domain with HTTPS
- **Monitoring**: UptimeRobot (Free)

**Total Cost**: ~$7/month for full production setup

---

## 🆘 **Troubleshooting**

### **Common Issues:**

**1. CORS Errors:**
- Update CORS origins in backend
- Check API URLs in dashboard

**2. Environment Variables:**
- Verify all required vars are set
- Check spelling and format

**3. Supabase Connection:**
- Verify URL and keys
- Check RLS policies
- Confirm schema is applied

**4. Build Failures:**
- Check Node.js version (18+)
- Verify package.json scripts
- Review build logs

---

Ready to deploy! Choose your preferred platform and follow the steps above. 🚀