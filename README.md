# 📱 Parental Control Platform

A comprehensive, production-ready parental control system with real-time monitoring and remote control capabilities.

## 🎯 **Features**

### **📱 Android App (Child Device)**
- **Comprehensive Monitoring**: Calls, SMS, apps, web activity, location, keyboard input
- **Real-time GPS Tracking**: Location updates every 2 minutes with address resolution
- **Remote Control**: Parent can remotely activate camera, record audio, get location
- **Secure Pairing**: 6-digit codes with 24-hour expiry for parent-child connection
- **Privacy Compliant**: Explicit consent required, transparent disclosure

### **💻 Web Dashboard (Parent Interface)**
- **Real-time Monitoring**: Live activity feed with filtering and search
- **Remote Commands**: Activate camera, record audio, force location update
- **Device Management**: Pair multiple child devices, view statistics
- **Secure Authentication**: Parent-only access with Supabase Auth

### **🌐 Cloud Backend**
- **Supabase Integration**: Real-time database with Row Level Security
- **Multi-platform Deployment**: Render, Vercel, Netlify, Docker support
- **Production Ready**: Health checks, rate limiting, security headers

## 🚀 **Quick Start**

### **1. Deploy Backend**

**Option A: Render (Recommended)**
1. Fork this repository
2. Connect to [Render](https://render.com)
3. Deploy using `render.yaml` configuration
4. Add environment variables (see below)

**Option B: Docker**
```bash
docker-compose up -d
```

### **2. Setup Supabase Database**
1. Create project at [Supabase](https://supabase.com)
2. Run `supabase/schema-update.sql` in SQL Editor
3. Create storage buckets: `monitoring-images`, `monitoring-audio`

### **3. Build Android APK**
1. Install [Android Studio](https://developer.android.com/studio)
2. Open `android-app` project
3. Build APK: **Build** → **Build APK(s)**
4. Install on child device

## ⚙️ **Configuration**

### **Environment Variables**
```env
SUPABASE_URL=your-supabase-url
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
NODE_ENV=production
PORT=3000
```

### **Supabase Setup**
- URL: `https://gejzprqznycnbfzeaxza.supabase.co`
- Run SQL schema from `supabase/schema-update.sql`
- Enable Row Level Security
- Create storage buckets for media files

## 📁 **Project Structure**

```
├── android-app/                 # Android monitoring app
│   ├── src/main/java/           # Java source code (25+ classes)
│   ├── src/main/res/            # Android resources
│   └── build.gradle             # Android build configuration
├── web-dashboard/               # Parent web interface
│   ├── index.html              # Dashboard UI
│   └── dashboard.js            # Frontend logic
├── backend/                     # Express.js API server
│   ├── server.js               # Main server file
│   └── package.json            # Node.js dependencies
├── supabase/                    # Database schema
│   ├── schema.sql              # Initial schema
│   └── schema-update.sql       # Production schema
├── deployment configs           # Multi-platform deployment
│   ├── render.yaml             # Render deployment
│   ├── vercel.json             # Vercel deployment
│   ├── netlify.toml            # Netlify deployment
│   └── docker-compose.yml      # Docker deployment
└── docs/                        # Setup guides
    ├── android-studio-setup.md  # Android Studio guide
    ├── deployment-guide.md      # Deployment instructions
    └── setup-complete.md        # Complete setup guide
```

## 🔧 **Android App Features**

### **Monitoring Capabilities**
- 📞 **Call Logs & Recording**: All calls with optional audio recording
- 💬 **SMS & Messages**: Text messages and app notifications
- ⌨️ **Keyboard Input**: All text input across applications
- 🌐 **Web Activity**: Browser history and app usage
- 📍 **GPS Tracking**: Real-time location with address resolution
- 📷 **Camera Monitoring**: Usage detection + remote activation
- 🎤 **Audio Monitoring**: Microphone usage + remote recording
- 🔔 **Notifications**: All app notifications and alerts
- 👆 **Screen Interactions**: Clicks, scrolls, navigation

### **Remote Control Features**
- 📸 **Remote Camera**: Parent can activate camera and view photos
- 🎤 **Remote Audio**: Record ambient audio for safety checks
- 📍 **Force Location**: Immediate GPS update on demand
- 🚨 **Emergency Alerts**: Safety status checks and notifications

## 🛡️ **Security & Privacy**

### **Legal Compliance**
- ✅ Explicit consent required before monitoring
- ✅ Transparent disclosure of all tracked activities
- ✅ Parental authorization verification
- ✅ Consent revocation mechanism

### **Technical Security**
- ✅ Row Level Security in Supabase
- ✅ Encrypted data transmission (HTTPS)
- ✅ Secure authentication with JWT
- ✅ API rate limiting and protection
- ✅ Input validation and sanitization

## 📱 **Installation & Usage**

### **For Parents:**
1. Access web dashboard at your deployed URL
2. Register account and verify email
3. Get 6-digit pairing code from child's device
4. Enter code to pair and start monitoring

### **For Child Device:**
1. Install APK on Android device (7.0+)
2. Complete permission setup (all-at-once flow)
3. Generate pairing code for parent
4. Review and grant consent
5. Monitoring starts automatically

## 🌐 **Deployment Options**

| Platform | Cost | Features |
|----------|------|----------|
| **Render** | $7/month | Auto-deploy, SSL, monitoring |
| **Vercel** | Free | Serverless, global CDN |
| **Netlify** | Free | Static hosting, forms |
| **Docker** | Self-hosted | Full control, local deployment |

## 📊 **API Endpoints**

### **Authentication**
- `POST /api/register` - Parent registration
- `POST /api/login` - Parent login

### **Device Management**
- `POST /api/device/register` - Device pairing
- `GET /api/devices` - List paired devices
- `POST /api/device/pair` - Pair with parent

### **Activity Monitoring**
- `POST /api/activity` - Log activity
- `GET /api/activities/:deviceId` - Get activities
- `GET /api/stats/:deviceId` - Device statistics

### **Remote Control**
- `POST /api/remote/camera` - Activate camera
- `POST /api/remote/audio` - Start audio recording
- `POST /api/remote/location` - Force location update

## 🧪 **Testing**

### **Backend Testing**
```bash
# Health check
curl https://your-api.com/health

# Test registration
curl -X POST https://your-api.com/api/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

### **Android Testing**
- Install APK on test device
- Complete permission setup
- Generate and use pairing code
- Verify monitoring activities
- Test remote control features

## 📈 **Performance**

### **Optimizations**
- Database indexing for fast queries
- Image/audio compression for uploads
- Batch processing for activities
- Caching for frequently accessed data

### **Scalability**
- Supabase auto-scaling database
- CDN delivery for static assets
- Load balancing ready architecture
- Horizontal scaling support

## 🤝 **Contributing**

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 **License**

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## ⚠️ **Legal Notice**

This software is designed for legitimate parental control purposes with explicit consent. Users are responsible for complying with local laws and regulations regarding monitoring and privacy. Always obtain proper consent before monitoring any device.

## 🆘 **Support**

- 📖 **Documentation**: Check the `docs/` folder for detailed guides
- 🐛 **Issues**: Report bugs via GitHub Issues
- 💬 **Discussions**: Use GitHub Discussions for questions

## 🎯 **Roadmap**

- [ ] iOS app development
- [ ] Advanced analytics dashboard
- [ ] Machine learning for behavior analysis
- [ ] Multi-language support
- [ ] Enterprise features

---

**Built with ❤️ for family safety and digital wellbeing**

## 📞 **Quick Links**

- [Android Studio Setup Guide](android-studio-setup.md)
- [Deployment Guide](deployment-guide.md)
- [Complete Setup Guide](setup-complete.md)
- [Supabase Schema](supabase/schema-update.sql)

**Ready to deploy? Follow the [Deployment Guide](deployment-guide.md) to get started!** 🚀