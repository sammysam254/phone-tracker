# Supabase Setup Guide for Parental Control Platform

## 1. Create Supabase Project

1. Go to [supabase.com](https://supabase.com) and create a new account
2. Click "New Project"
3. Choose your organization
4. Enter project name: "parental-control"
5. Enter database password (save this securely)
6. Select region closest to your users
7. Click "Create new project"

## 2. Database Setup

1. Go to the SQL Editor in your Supabase dashboard
2. Copy and paste the contents of `supabase/schema.sql`
3. Click "Run" to execute the SQL commands

This will create:
- `devices` table for storing device information
- `activities` table for storing monitored activities
- Row Level Security policies for data protection
- Indexes for better performance
- Helper functions for statistics

## 3. Get API Keys

1. Go to Settings → API in your Supabase dashboard
2. Copy the following values:
   - Project URL
   - `anon` `public` key
   - `service_role` `secret` key (keep this secure!)

## 4. Configure Environment Variables

### Backend (.env file)
```env
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
PORT=3000
NODE_ENV=development
```

### Web Dashboard (dashboard.js)
Update the constants at the top of the file:
```javascript
const SUPABASE_URL = 'https://your-project-id.supabase.co';
const SUPABASE_ANON_KEY = 'your-anon-key';
```

### Android App (SupabaseClient.java)
Update the constants:
```java
private static final String SUPABASE_URL = "https://your-project-id.supabase.co";
private static final String SUPABASE_ANON_KEY = "your-anon-key";
```

## 5. Authentication Setup

1. Go to Authentication → Settings in Supabase dashboard
2. Enable email authentication
3. Configure email templates if needed
4. Set up redirect URLs for your web dashboard

## 6. Row Level Security

The schema includes RLS policies that ensure:
- Parents can only see their own devices
- Parents can only see activities from their devices
- Device activities can only be inserted if consent is granted
- All data access is properly authenticated

## 7. Real-time Features (Optional)

To enable real-time updates:
1. Go to Database → Replication in Supabase dashboard
2. Enable replication for `activities` table
3. Update web dashboard to use Supabase real-time subscriptions

## 8. Testing the Setup

1. Start the backend server: `npm run dev`
2. Open the web dashboard in a browser
3. Register a parent account
4. Add a test device
5. Install and run the Android app
6. Grant consent and permissions
7. Verify activities appear in the dashboard

## 9. Production Deployment

### Backend
- Deploy to Vercel, Netlify, or your preferred platform
- Set environment variables in deployment settings
- Update CORS settings if needed

### Web Dashboard
- Deploy static files to any web hosting service
- Update Supabase URLs to production values
- Configure custom domain if needed

### Android App
- Update Supabase URLs to production values
- Build signed APK for distribution
- Test thoroughly on different devices

## Security Considerations

1. **Never expose service role key** in client-side code
2. **Use anon key** for client-side operations only
3. **Enable RLS** on all tables containing sensitive data
4. **Validate all inputs** on both client and server side
5. **Use HTTPS** for all communications
6. **Regularly rotate** API keys and passwords
7. **Monitor usage** through Supabase dashboard

## Monitoring and Maintenance

1. Set up monitoring alerts in Supabase dashboard
2. Monitor database performance and usage
3. Regularly backup your database
4. Keep Supabase client libraries updated
5. Review and update RLS policies as needed