# Power BI Embedded Analytics Platform

**Project Type:** Cloud-based SaaS Web Application
**Development Model:** Full-stack (React TypeScript + Java Spring Boot)
**Hourly Rate:** $15/hour
**Developer:** Oussama Benberkane

---

## TABLE OF CONTENTS

1. [Project Overview](#1-project-overview)
2. [User Roles & Workflows](#2-user-roles--workflows)
3. [Frontend Development Breakdown](#3-frontend-development-breakdown)
4. [Backend Development Breakdown](#4-backend-development-breakdown)
5. [Integration & Testing](#5-integration--testing)
6. [Deployment & DevOps](#6-deployment--devops)
7. [Total Estimates](#7-total-estimates)
8. [Notes & Assumptions](#notes--assumptions)
9. [Payment Terms](#payment-terms-proposal)

---

## 1. PROJECT OVERVIEW

### Description

A cloud-based SaaS platform that delivers Power BI dashboards to clients (individuals or organizations). Clients sign up through the app, and the SuperAdmin activates accounts, creates Power BI workspaces for them, and publishes their dashboards. The organization account's main user can invite other users as team members, while individuals use the platform solo.

### Tech Stack

- **Frontend:** React + TypeScript + Tailwind CSS
- **Backend:** Java Spring Boot + PostgreSQL
- **ETL:** Python scripts (Dockerized)
- **BI Layer:** Power BI Embedded
- **Hosting:** Cloud VPS (Docker)
- **Authentication:** JWT

### User Roles

- **SuperAdmin:** Platform owner, manages all clients, subscriptions and dashboards
- **Individual User:** Single person account, views dashboards
- **Organization User:** Part of an organization, views dashboards; main contact can invite team members

---

## 2. USER ROLES & WORKFLOWS

### Workflow A: Individual Sign-up

1. User signs up on app (individual form) → Account status: "Pending"
2. SuperAdmin receives notification in admin panel
3. SuperAdmin calls user, gathers data source info
4. SuperAdmin activates account → Creates Power BI workspace, assigns subscription plan
5. User logs in, views dashboards

### Workflow B: Organization Sign-up

1. Main contact signs up on app (organization form) → Account status: "Pending"
2. SuperAdmin receives notification in admin panel
3. SuperAdmin calls contact, gathers data source info
4. SuperAdmin activates organization → Creates Power BI workspace, assigns plan
5. The backend auto-creates the first user account: "OrganizationName User"
6. Main contact logs in with this account, updates profile (name, email, password)
7. Main contact invites team members (creates additional user accounts)
8. All organization users view the same dashboards

### Workflow C: Dashboard Publishing (SuperAdmin)

1. SuperAdmin's daughter sets up Python ETL script for client's data source and communicates it to backend dev
2. Script extracts data and pushes to Power BI dataset
3. SuperAdmin creates dashboards in Power BI
4. SuperAdmin publishes dashboards to client's workspace
5. Client users can now view and interact with dashboards

---

## 3. FRONTEND DEVELOPMENT BREAKDOWN

**Rate:** $15/hour

### 3.1 Shared Authentication Module

| Task | Description | Hours | Cost |
|------|-------------|-------|------|
| Sign-up form (Individual) | Form with personal info fields, validation | 0.5 | $7.50 |
| Sign-up form (Organization) | Form with org details, main contact info, validation | 0.5 | $7.50 |
| Login page | Email/password login, JWT handling | 1.5 | $22.50 |
| Password reset flow | Request reset, reset form | 1.0 | $15.00 |
| Profile management | Update name, email, other information | 1.0 | $15.00 |
| Auth state management | Context/Redux for auth state | 2.0 | $30.00 |
| **Subtotal** | | **6.5** | **$97.50** |

### 3.2 SuperAdmin Panel (admin.yourapp.com)

#### 3.2.1 Dashboard & Navigation

| Task | Hours | Cost |
|------|-------|------|
| Admin layout & navigation | 1.5 | $22.50 |
| Dashboard home | 10.0 | $150.00 |
| **Subtotal** | **11.5** | **$172.50** |

#### 3.2.2 Pending Accounts Management

| Task | Hours | Cost |
|------|-------|------|
| Pending accounts list | 4.0 | $60.00 |
| Account detail view | 2.0 | $30.00 |
| Activation / Rejection/deletion form | 2.0 | $30.00 |
| **Subtotal** | **8.0** | **$120.00** |

#### 3.2.3 Client Management

| Task | Hours | Cost |
|------|-------|------|
| Active clients list | 2.0 | $30.00 |
| Client detail page | 0.0 | $0.00 |
| Edit client | 1.0 | $15.00 |
| Suspend/reactivate/delete client | 1.5 | $22.50 |
| **Subtotal** | **4.5** | **$67.50** |

#### 3.2.4 User Management (for Organizations)

| Task | Hours | Cost |
|------|-------|------|
| Organization users list | 2.0 | $30.00 |
| Add user to organization | 1.0 | $15.00 |
| Edit/delete organization user | 0.5 | $7.50 |
| **Subtotal** | **3.5** | **$52.50** |

#### 3.2.5 Data Source Configuration

| Task | Hours | Cost |
|------|-------|------|
| Data source form | 1.0 | $15.00 |
| Data source list per client | 0.5 | $7.50 |
| Edit/update data source | 0.5 | $7.50 |
| Test connection button | 0.0 | $0.00 |
| **Subtotal** | **2.0** | **$30.00** |

#### 3.2.6 Dashboard Management

| Task | Hours | Cost |
|------|-------|------|
| Dashboard list per client | 1.0 | $15.00 |
| Publish dashboard form | 2.0 | $30.00 |
| Update/republish dashboard | 2.0 | $30.00 |
| Delete dashboard | 0.0 | $0.00 |
| **Subtotal** | **5.0** | **$75.00** |

#### 3.2.7 Subscription Plans Management

| Task | Hours | Cost |
|------|-------|------|
| Plans list | 2.0 | $30.00 |
| Create/edit plan | 1.0 | $15.00 |
| Delete plan | 0.0 | $0.00 |
| **Subtotal** | **3.0** | **$45.00** |

#### 3.2.8 Platform Analytics

| Task | Hours | Cost |
|------|-------|------|
| Usage dashboard | 8.0 | $120.00 |
| Power BI capacity monitoring | 1.0 | $15.00 |
| Export analytics | 0.0 | $0.00 |
| **Subtotal** | **9.0** | **$135.00** |

**SuperAdmin Panel Total:** 46.5 hrs → **$697.50**

### 3.3 Client App (app.yourapp.com)

*Mobile-First Design*

#### 3.3.1 Client Dashboard

| Task | Hours | Cost |
|------|-------|------|
| Client layout & navigation | 2.0 | $30.00 |
| Dashboard list view | 4.0 | $60.00 |
| Dashboard search/filter | 1.0 | $15.00 |
| **Subtotal** | **7.0** | **$105.00** |

#### 3.3.2 Dashboard Viewer

| Task | Hours | Cost |
|------|-------|------|
| Embedded Power BI viewer | 3.0 | $45.00 |
| Filter controls | 1.0 | $15.00 |
| Export functionality | 0.0 | $0.00 |
| Full-screen mode | 1.0 | $15.00 |
| **Subtotal** | **5.0** | **$75.00** |

#### 3.3.3 Team Management (Organization Users Only)

| Task | Hours | Cost |
|------|-------|------|
| Team members list | 1.0 | $15.00 |
| Invite user form | 1.0 | $15.00 |
| Remove team member | 0.0 | $0.00 |
| **Subtotal** | **2.0** | **$30.00** |

#### 3.3.4 Settings & Profile

| Task | Hours | Cost |
|------|-------|------|
| User profile page | 1.0 | $15.00 |
| Change password | 1.0 | $15.00 |
| **Subtotal** | **2.0** | **$30.00** |

**Client App Total:** 16.0 hrs → **$240.00**

### 3.4 UI Components with State Management

| Task | Hours | Cost |
|------|-------|------|
| Loading states | 3.0 | $45.00 |
| Error handling UI | 3.0 | $45.00 |
| **Subtotal** | **6.0** | **$90.00** |

### 3.5 Testing & Optimization

| Task | Hours | Cost |
|------|-------|------|
| Testing & Optimization | 5.0 | $75.00 |

### FRONTEND TOTAL ESTIMATE

| Category | Hours | Cost |
|----------|-------|------|
| Authentication Module | 6.5 | $97.50 |
| SuperAdmin Panel | 46.5 | $697.50 |
| Client App | 16.0 | $240.00 |
| UI Components & Styling | 6.0 | $90.00 |
| Testing & Optimization | 5.0 | $75.00 |
| **TOTAL FRONTEND** | **80.0** | **$1,200.00** |

---

## 4. BACKEND DEVELOPMENT BREAKDOWN

**Rate:** $15/hour

### 4.1 Database Schema & Models

| Task | Hours | Cost |
|------|-------|------|
| Users table | 0.0 | $0.00 |
| Organizations table | 0.5 | $7.50 |
| Clients table | 1.0 | $15.00 |
| SubscriptionPlans table | 0.5 | $7.50 |
| Subscriptions table | 0.5 | $7.50 |
| DataSources table | 0.5 | $7.50 |
| Dashboards table | 1.0 | $15.00 |
| PowerBIWorkspaces table | 1.5 | $22.50 |
| AuditLogs table | 0.0 | $0.00 |
| Database migrations | 0.0 | $0.00 |
| **Subtotal** | **5.5** | **$82.50** |

### 4.2 Authentication & Authorization

| Task | Hours | Cost |
|------|-------|------|
| JWT implementation | 0.0 | $0.00 |
| Login endpoint | 0.0 | $0.00 |
| Password hashing | 0.0 | $0.00 |
| Role-based access control | 0.0 | $0.00 |
| Security config | 0.0 | $0.00 |
| Password reset | 1.0 | $15.00 |
| **Subtotal** | **1.0** | **$15.00** |

### 4.3 Multi-Tenancy Implementation

| Task | Hours | Cost |
|------|-------|------|
| Tenant context | 3.0 | $45.00 |
| Tenant interceptor | 0.0 | $0.00 |
| Tenant filtering | 1.5 | $22.50 |
| Testing | 3.0 | $45.00 |
| **Subtotal** | **7.5** | **$112.50** |

### 4.4 User Management API

| Task | Hours | Cost |
|------|-------|------|
| User registration | 1.0 | $15.00 |
| User login | 1.0 | $15.00 |
| Get current user | 0.0 | $0.00 |
| Update user profile | 0.5 | $7.50 |
| Change password | 0.5 | $7.50 |
| Password reset request | 0.25 | $3.75 |
| Password reset confirm | 0.25 | $3.75 |
| **Subtotal** | **3.5** | **$52.50** |

### 4.5 Organization & Team Management API

| Task | Hours | Cost |
|------|-------|------|
| Create organization | 1.0 | $15.00 |
| Get organization | 0.25 | $3.75 |
| Update organization | 0.25 | $3.75 |
| Delete organization | 0.25 | $3.75 |
| List organization users | 0.5 | $7.50 |
| Invite user to organization | 1.0 | $15.00 |
| Remove user from organization | 0.25 | $3.75 |
| Accept invitation | 0.5 | $7.50 |
| **Subtotal** | **4.0** | **$60.00** |

### 4.6 Power BI Integration

| Task | Hours | Cost |
|------|-------|------|
| Power BI SDK integration | 3.0 | $45.00 |
| Workspace creation | 3.0 | $45.00 |
| Workspace management | 3.0 | $45.00 |
| Report publishing | 2.0 | $30.00 |
| Embed token generation | 1.0 | $15.00 |
| Get embed config | 0.5 | $7.50 |
| RLS implementation | 3.0 | $45.00 |
| **Subtotal** | **15.5** | **$232.50** |

### 4.7 Data Source Management API

| Task | Hours | Cost |
|------|-------|------|
| Create data source | 0.25 | $3.75 |
| List data sources | 0.5 | $7.50 |
| Get data source | 0.25 | $3.75 |
| Update data source | 0.25 | $3.75 |
| Delete data source | 0.25 | $3.75 |
| Test connection | 2.0 | $30.00 |
| Credential encryption | 0.0 | $0.00 |
| **Subtotal** | **3.5** | **$52.50** |

### 4.8 Subscription & Billing API

| Task | Hours | Cost |
|------|-------|------|
| Create subscription plan | 0.25 | $3.75 |
| List subscription plans | 0.5 | $7.50 |
| Update subscription plan | 0.25 | $3.75 |
| Delete subscription plan | 0.25 | $3.75 |
| Assign subscription | 0.5 | $7.50 |
| Update subscription | 0.25 | $3.75 |
| Check subscription limits | 0.25 | $3.75 |
| **Subtotal** | **2.25** | **$33.75** |

### 4.9 Analytics & Reporting API

| Task | Hours | Cost |
|------|-------|------|
| Platform metrics endpoint | 2.0 | $30.00 |
| Usage analytics | 3.0 | $45.00 |
| Power BI capacity metrics | 5.0 | $75.00 |
| Export analytics | 2.0 | $30.00 |
| **Subtotal** | **12.0** | **$180.00** |

### 4.10 Email Service

| Task | Hours | Cost |
|------|-------|------|
| Email configuration | 0.25 | $3.75 |
| Password reset email | 0.25 | $3.75 |
| Invitation email | 0.25 | $3.75 |
| Account activation email | 0.25 | $3.75 |
| **Subtotal** | **1.0** | **$15.00** |

### 4.11 Python ETL Scripts Management

| Task | Hours | Cost |
|------|-------|------|
| ETL script storage | 0.0 | $0.00 |
| Script execution endpoint | 0.5 | $7.50 |
| Docker environment setup | 0.0 | $0.00 |
| Credentials management | 0.25 | $3.75 |
| Scheduled execution | 2.0 | $30.00 |
| **Subtotal** | **2.75** | **$41.25** |

### 4.12 Security Implementation

| Task | Hours | Cost |
|------|-------|------|
| Password encryption | 0.0 | $0.00 |
| Data source credential encryption | 0.0 | $0.00 |
| HTTPS enforcement | 2.0 | $30.00 |
| Input validation | 0.0 | $0.00 |
| **Subtotal** | **2.0** | **$30.00** |

### BACKEND TOTAL ESTIMATE

| Category | Hours | Cost |
|----------|-------|------|
| Database Schema & Models | 5.5 | $82.50 |
| Authentication & Authorization | 1.0 | $15.00 |
| Multi-Tenancy Implementation | 7.5 | $112.50 |
| User Management API | 3.5 | $52.50 |
| Organization & Team Management API | 4.0 | $60.00 |
| Power BI Integration | 15.5 | $232.50 |
| Data Source Management API | 3.5 | $52.50 |
| Subscription & Billing API | 2.25 | $33.75 |
| Analytics & Reporting API | 12.0 | $180.00 |
| Email Service | 1.0 | $15.00 |
| Python ETL Scripts Management | 2.75 | $41.25 |
| Security Implementation | 2.0 | $30.00 |
| **TOTAL BACKEND** | **60.5** | **$907.50** |

---

## 5. INTEGRATION & TESTING

| Task | Hours | Cost |
|------|-------|------|
| API integration testing | 20.0 | $300.00 |
| Power BI embedding testing | 5.0 | $75.00 |
| Cross-browser & mobile responsiveness | 5.0 | $75.00 |
| Bug fixes & refinements | 0.0 | $0.00 |
| **TOTAL** | **30.0** | **$450.00** |

---

## 6. DEPLOYMENT & DEVOPS

| Task | Hours | Cost |
|------|-------|------|
| VPS setup | 2.0 | $30.00 |
| Docker deployment | 3.0 | $45.00 |
| Domain configuration | 1.0 | $15.00 |
| SSL certificates | 2.0 | $30.00 |
| Database hosting | 1.0 | $15.00 |
| Backup strategy | 2.0 | $30.00 |
| **TOTAL** | **11.0** | **$165.00** |

---

## 7. TOTAL ESTIMATES

| Phase | Hours | Cost @ $15/hr |
|-------|-------|---------------|
| Frontend Development | 80.0 | $1,200.00 |
| Backend Development | 60.5 | $907.50 |
| Integration & Testing | 30.0 | $450.00 |
| Deployment & DevOps | 11.0 | $165.00 |
| **GRAND TOTAL** | **181.5** | **$2,722.50** |

### Estimated Project Duration

At **3 hours/day**: 181.5 ÷ 3 ≈ **60 days** → ~**8 weeks** / ~**2 months**

---

## NOTES & ASSUMPTIONS

- **Third-party Services:** Email service (e.g., SendGrid) costs not included
- **Hosting Costs:** VPS monthly fees not included in development estimate
- **Python Scripts:** ETL script development is assumed to be done by SuperAdmin's daughter, not included in this estimate
- **Content:** All text content (terms, privacy policy, etc.) provided by client
- **Maintenance:** Post-launch maintenance and support not included

---

## PAYMENT TERMS (PROPOSAL)

- **Deposit:** 10% upfront
- **Platform ready and tested:** 40%
- **Final Payment:** 50% upon deployment

---

**Document Prepared By:** Oussama Benberkane
**Date:** 22/10/2025
**Version:** 1.0
