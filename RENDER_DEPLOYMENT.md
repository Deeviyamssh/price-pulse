# Render Deployment Guide

This guide will help you deploy PricePulse to Render.com.

## Architecture for Render

For the best deployment experience on Render, we recommend:
- **Backend**: Deployed to Render (Docker)
- **Frontend**: Deployed to Vercel or Netlify (Static site with build-time env vars)
- **Database**: Render PostgreSQL

This separation gives you:
- Free tier on both platforms
- Better support for build-time environment variables
- Faster frontend deployments
- Better caching and CDN

---

## Step 1: Deploy Backend to Render

### 1.1 Push Code to GitHub

```bash
git add .
git commit -m "Add Render deployment configuration"
git push origin main
```

### 1.2 Create Render Account

1. Go to [render.com](https://render.com)
2. Sign up with GitHub
3. Authorize Render to access your repositories

### 1.3 Deploy PostgreSQL Database

1. In Render dashboard, click **New +**
2. Select **PostgreSQL**
3. Configure:
   - Name: `price-pulse-db`
   - Database: `pricepulse`
   - User: `pricepulse`
   - Region: Choose closest to you
   - Plan: Free (recommended for testing)
4. Click **Create Database**

**Save these credentials** (you'll need them):
- Internal Database URL
- Database Password

### 1.4 Deploy Backend Service

1. In Render dashboard, click **New +**
2. Select **Web Service**
3. Connect your GitHub repository
4. Configure:
   - Name: `price-pulse-backend`
   - Environment: Docker
   - Dockerfile Path: `./backend/Dockerfile`
   - Branch: `main`
   - Region: Same as database
   - Plan: Free
5. Add Environment Variables:
   - `SPRING_PROFILES_ACTIVE`: `render`
   - `SPRING_DATASOURCE_URL`: `[From database connection string]`
   - `SPRING_DATASOURCE_USERNAME`: `pricepulse`
   - `SPRING_DATASOURCE_PASSWORD`: `[From database]`
   - `JWT_SECRET`: `[Generate a random 32+ character string]`
   - `PRICEPULSE_SCHEDULER_INTERVAL_MS`: `1800000`
   - `SPRING_MAIL_HOST`: `smtp.gmail.com` (or your SMTP)
   - `SPRING_MAIL_PORT`: `587`
   - `SPRING_MAIL_USERNAME`: `[Your email]`
   - `SPRING_MAIL_PASSWORD`: `[Your app password]`
6. Click **Create Web Service**

Render will build and deploy your backend. Once complete, you'll get a URL like:
`https://price-pulse-backend.onrender.com`

---

## Step 2: Deploy Frontend to Vercel (Recommended)

### 2.1 Create Vercel Account

1. Go to [vercel.com](https://vercel.com)
2. Sign up with GitHub
3. Authorize Vercel to access your repositories

### 2.2 Deploy Frontend

1. In Vercel dashboard, click **Add New Project**
2. Import your GitHub repository
3. Configure:
   - Framework Preset: Vite
   - Root Directory: `frontend`
   - Build Command: `npm run build`
   - Output Directory: `dist`
4. Add Environment Variable:
   - `VITE_API_BASE`: `https://price-pulse-backend.onrender.com`
5. Click **Deploy**

Vercel will build and deploy your frontend. You'll get a URL like:
`https://price-pulse-frontend.vercel.app`

---

## Step 3: Configure Email (Production)

For production email, you'll need to set up SMTP:

### Option 1: Gmail (Free)

1. Enable 2FA on your Google account
2. Generate an App Password:
   - Go to Google Account Settings > Security
   - Enable 2-Step Verification
   - Generate App Password for "Mail"
3. Use these credentials in Render:
   - `SPRING_MAIL_HOST`: `smtp.gmail.com`
   - `SPRING_MAIL_PORT`: `587`
   - `SPRING_MAIL_USERNAME`: `[Your Gmail address]`
   - `SPRING_MAIL_PASSWORD`: `[Your App Password]`

### Option 2: SendGrid (Free tier available)

1. Sign up at [sendgrid.com](https://sendgrid.com)
2. Create an API key
3. Use SendGrid SMTP credentials in Render

---

## Step 4: Test Your Deployment

1. Open your frontend URL (Vercel)
2. Register a new account
3. Login
4. Add a product
5. Set a price alert
6. Verify everything works

---

## Troubleshooting

### Backend Not Starting

Check Render logs:
1. Go to your backend service in Render
2. Click **Logs**
3. Look for error messages

Common issues:
- Database connection failed → Check `SPRING_DATASOURCE_URL`
- JWT_SECRET too short → Must be 32+ characters
- Port binding issue → Backend should use port 8080

### Frontend Can't Connect to Backend

1. Check `VITE_API_BASE` in Vercel
2. Verify backend URL is correct
3. Check browser console for CORS errors
4. Ensure backend is running

### Email Not Sending

1. Verify SMTP credentials
2. Check Render logs for email errors
3. For Gmail, ensure you're using App Password (not regular password)

---

## Cost Summary

- **Render Backend**: Free tier (750 hours/month)
- **Render PostgreSQL**: Free tier (90 days, then $7/month)
- **Vercel Frontend**: Free tier (unlimited static sites)
- **Total**: Free for testing, ~$7/month for production database

---

## Alternative: All-in-One on Render

If you prefer to deploy everything to Render:

1. Deploy backend and database as above
2. Deploy frontend as a separate Render web service:
   - Runtime: Node
   - Build Command: `cd frontend && npm install && npm run build`
   - Start Command: `cd frontend && npx serve -s dist -l 3000`
   - Add `VITE_API_BASE` environment variable

Note: This approach uses the free tier but may have cold starts and slower performance than Vercel.
