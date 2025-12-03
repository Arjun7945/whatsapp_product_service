# WhatsApp Product Service - Visual Flow Diagrams

> **Note**: These diagrams use Mermaid syntax and can be viewed in:
> - GitHub (renders automatically)
> - VS Code with Mermaid extension
> - Online at https://mermaid.live (copy and paste the code)
> - Markdown preview tools

---

## 1. Complete Customer Order to Delivery Flow

```mermaid
graph TB
    subgraph Customer["👤 CUSTOMER"]
        C1[Send 'Hi'] --> C2[Register<br/>Name, Phone, Location]
        C2 --> C3[Send 'start']
        C3 --> C4[Browse Products]
        C4 --> C5[Select Fish + Quantity]
        C5 --> C6[Click 'Checkout']
        C6 --> C7[Click 'Confirm Order']
        C7 -.->|Receives| C8[✅ Order Confirmed #123]
        C8 -.->|Later receives| C9[🚚 Delivery Person<br/>Assigned!]
    end

    subgraph Backend["⚙️ BACKEND SYSTEM"]
        B1[Create Order<br/>Status: PENDING]
        B2[(Save to<br/>Database)]
        B3[Send Order to<br/>Delivery Group]
        B4{Validate<br/>Delivery Person<br/>Role}
        B5[Update Order<br/>Status: CONFIRMED]
        B6[Save Delivery<br/>Person Details]
    end

    subgraph DeliveryGroup["📱 DELIVERY GROUP"]
        D1[Receive Order<br/>Notification]
        D2[🔔 NEW ORDER #123<br/>+ Confirm Button]
        D3[Delivery Person<br/>Clicks Button]
        D4[Receive Confirmation]
        D5[✅ ORDER CONFIRMED<br/>Name: Rajesh<br/>Phone: 9876543210]
    end

    C7 ==>|10:00 AM| B1
    B1 --> B2
    B2 --> B3
    B3 ==>|10:00 AM| D1
    D1 --> D2
    D2 --> D3
    D3 ==>|10:05 AM| B4
    B4 -->|✅ DELIVERY_PERSON<br/>role verified| B5
    B5 --> B6
    B6 -.->|Notify| C9
    B6 ==>|10:05 AM| D4
    D4 --> D5

    B4 -.->|❌ Unauthorized| X[Rejection Message]

    style C1 fill:#E3F2FD
    style C7 fill:#2196F3,color:#fff
    style C8 fill:#4CAF50,color:#fff
    style C9 fill:#4CAF50,color:#fff
    style B1 fill:#FFF9C4
    style B4 fill:#FFE082
    style B5 fill:#81C784,color:#fff
    style B6 fill:#81C784,color:#fff
    style D2 fill:#FFB74D,color:#fff
    style D3 fill:#FF9800,color:#fff
    style D5 fill:#66BB6A,color:#fff
    style X fill:#EF5350,color:#fff
```

---

## 2. Delivery Person Confirmation Logic (Detailed)

```mermaid
flowchart TD
    Start([Delivery Person Clicks<br/>'Confirm Delivery']) --> Check1{Order Status<br/>= CONFIRMED?}
    
    Check1 -->|YES ❌| End1[Send: Order already<br/>taken by other person]
    Check1 -->|NO ✅| Check2{User exists in<br/>team_members<br/>table?}
    
    Check2 -->|NO ❌| End2[Send: Unauthorized Access<br/>You are not registered<br/>as delivery person]
    Check2 -->|YES ✅| Check3{User Role<br/>= DELIVERY_PERSON?}
    
    Check3 -->|NO ❌| End3[Send: Unauthorized Access<br/>Wrong role - Only delivery<br/>persons can confirm]
    Check3 -->|YES ✅| Process1[Update Order Status<br/>PENDING → CONFIRMED]
    
    Process1 --> Process2[Save Delivery Person ID]
    Process2 --> Process3[Save Delivery Person Name]
    Process3 --> Process4[Save Delivery Person<br/>WhatsApp ID]
    Process4 --> Process5[Set confirmed_at<br/>timestamp]
    Process5 --> Process6[(Save to Database)]
    
    Process6 --> Notify1[Send to Customer:<br/>🚚 Delivery Person Assigned!<br/>Your order will be delivered by NAME]
    Process6 --> Notify2[Send to Group:<br/>✅ ORDER CONFIRMED<br/>📦 Order #123 assigned<br/>👤 Name: Rajesh Kumar<br/>📞 Phone: 9876543210<br/>⏰ Confirmed at: 10:05 AM]
    
    Notify1 --> EndSuccess([End - Success])
    Notify2 --> EndSuccess
    
    End1 --> EndFail([End - Rejected])
    End2 --> EndFail
    End3 --> EndFail
    
    style Start fill:#2196F3,color:#fff
    style Check1 fill:#FFC107
    style Check2 fill:#FFC107
    style Check3 fill:#FFC107
    style Process1 fill:#4CAF50,color:#fff
    style Process2 fill:#4CAF50,color:#fff
    style Process3 fill:#4CAF50,color:#fff
    style Process4 fill:#4CAF50,color:#fff
    style Process5 fill:#4CAF50,color:#fff
    style Process6 fill:#66BB6A,color:#fff
    style Notify1 fill:#2196F3,color:#fff
    style Notify2 fill:#FF9800,color:#fff
    style End1 fill:#F44336,color:#fff
    style End2 fill:#F44336,color:#fff
    style End3 fill:#F44336,color:#fff
    style EndSuccess fill:#4CAF50,color:#fff
    style EndFail fill:#F44336,color:#fff
```

---

## 3. Message Routing Architecture

```mermaid
flowchart TB
    Start([WhatsApp Message<br/>Received]) --> Webhook[WebhookController]
    Webhook --> Process[CustomerFlowService<br/>processIncomingMessage]
    
    Process --> CheckTeam{Sender in<br/>team_members<br/>table?}
    
    CheckTeam -->|NO| Customer[Treat as Customer]
    CheckTeam -->|YES| CheckRole{Check Role}
    
    Customer --> MsgType1{Message Type?}
    MsgType1 -->|Text| Flow1[Customer Text Flow<br/>Registration/Shopping]
    MsgType1 -->|Location| Flow2[Location Handler<br/>Validate Delivery Area]
    MsgType1 -->|Button Reply| Flow3[Button Handler<br/>Checkout/Confirm]
    MsgType1 -->|List Selection| Flow4[List Handler<br/>Product Selection]
    
    CheckRole -->|EXECUTIVE| ExecFlow[ExecutiveFlowService<br/>handleExecutiveMessage]
    CheckRole -->|DELIVERY_PERSON| MsgType2{Message Type?}
    
    MsgType2 -->|Button Reply<br/>DELIVERY_TAKE_*| DeliveryConfirm[DeliveryFlowService<br/>handleDeliveryConfirmation<br/>✅ MAIN DELIVERY LOGIC]
    MsgType2 -->|Other| Ack[Send: Please use<br/>confirmation buttons<br/>in the group]
    
    ExecFlow --> ExecOps[Add Customer<br/>View Customers]
    
    Flow1 --> End1([End])
    Flow2 --> End1
    Flow3 --> End1
    Flow4 --> End1
    ExecOps --> End1
    DeliveryConfirm --> End1
    Ack --> End1
    
    style Start fill:#9C27B0,color:#fff
    style Webhook fill:#673AB7,color:#fff
    style Process fill:#3F51B5,color:#fff
    style CheckTeam fill:#FFC107
    style CheckRole fill:#FFC107
    style Customer fill:#2196F3,color:#fff
    style Flow1 fill:#64B5F6,color:#fff
    style Flow2 fill:#64B5F6,color:#fff
    style Flow3 fill:#64B5F6,color:#fff
    style Flow4 fill:#64B5F6,color:#fff
    style ExecFlow fill:#00BCD4,color:#fff
    style ExecOps fill:#4DD0E1,color:#fff
    style DeliveryConfirm fill:#4CAF50,color:#fff,stroke:#2E7D32,stroke-width:4px
    style Ack fill:#FF9800,color:#fff
    style End1 fill:#9E9E9E,color:#fff
```

---

## 4. System Architecture Overview

```mermaid
graph LR
    subgraph Clients["📱 CLIENTS"]
        Customer[Customer<br/>WhatsApp]
        Executive[Executive<br/>WhatsApp]
        Delivery[Delivery Person<br/>WhatsApp]
    end
    
    subgraph WhatsAppAPI["☁️ WHATSAPP BUSINESS API"]
        WAPI[WhatsApp Cloud API<br/>graph.facebook.com]
    end
    
    subgraph Backend["⚙️ SPRING BOOT BACKEND"]
        Controller[WebhookController]
        
        subgraph Services["Services Layer"]
            CFS[CustomerFlowService]
            EFS[ExecutiveFlowService]
            DFS[DeliveryFlowService<br/>⭐ NEW]
            WAS[WhatsAppService]
            SCS[ShoppingCartService]
            LVS[LocationValidationService]
        end
        
        subgraph Repositories["Repository Layer"]
            Repos[(JPA Repositories)]
        end
    end
    
    subgraph Database["🗄️ DATABASE"]
        DB[(PostgreSQL<br/>fishdb)]
    end
    
    Customer <-->|Messages| WAPI
    Executive <-->|Messages| WAPI
    Delivery <-->|Messages| WAPI
    
    WAPI <-->|Webhooks| Controller
    
    Controller --> CFS
    Controller --> EFS
    Controller --> DFS
    
    CFS --> WAS
    CFS --> SCS
    CFS --> LVS
    CFS --> DFS
    
    EFS --> WAS
    DFS --> WAS
    
    CFS --> Repos
    EFS --> Repos
    DFS --> Repos
    SCS --> Repos
    
    Repos <--> DB
    
    WAS -->|Send Messages| WAPI
    
    style Customer fill:#2196F3,color:#fff
    style Executive fill:#00BCD4,color:#fff
    style Delivery fill:#FF9800,color:#fff
    style WAPI fill:#25D366,color:#fff
    style Controller fill:#673AB7,color:#fff
    style CFS fill:#3F51B5,color:#fff
    style EFS fill:#00BCD4,color:#fff
    style DFS fill:#4CAF50,color:#fff,stroke:#2E7D32,stroke-width:3px
    style WAS fill:#9C27B0,color:#fff
    style SCS fill:#7B1FA2,color:#fff
    style LVS fill:#6A1B9A,color:#fff
    style Repos fill:#FF6F00,color:#fff
    style DB fill:#F57C00,color:#fff
```

---

## 5. Database Schema Relationships

```mermaid
erDiagram
    CUSTOMER ||--o{ CUSTOMER_ORDER : "places"
    CUSTOMER_ORDER ||--|{ ORDER_ITEM : "contains"
    CUSTOMER_ORDER }o--|| TEAM_MEMBER : "assigned to"
    FISH_PRODUCT ||--o{ ORDER_ITEM : "ordered as"
    CUSTOMER ||--o{ CART_ITEM : "has"
    FISH_PRODUCT ||--o{ CART_ITEM : "in cart"
    
    CUSTOMER {
        bigint id PK
        string name
        string wa_phone_number UK
        string phone_number
        double location_lat
        double location_lon
        double distance_from_business_km
        enum current_flow_stage
        datetime registered_at
    }
    
    TEAM_MEMBER {
        bigint id PK
        string name
        string wa_phone_number UK
        string phone_number UK
        enum role "EXECUTIVE or DELIVERY_PERSON"
        boolean is_active
        enum current_flow_stage
    }
    
    CUSTOMER_ORDER {
        bigint id PK
        bigint customer_id FK
        datetime order_time
        double total_amount
        enum status "PENDING, CONFIRMED, DELIVERED"
        string payment_method
        bigint team_member_id FK "Delivery Person"
        string team_member_wa_id
        string team_member_name
        datetime confirmed_at "When DP confirmed"
    }
    
    ORDER_ITEM {
        bigint id PK
        bigint order_id FK
        bigint fish_product_id FK
        double quantity_kg
        double price_at_order
    }
    
    FISH_PRODUCT {
        bigint id PK
        string name
        double price_per_kg
        boolean is_available
        string image_url
    }
    
    CART_ITEM {
        bigint id PK
        bigint customer_id FK
        bigint fish_product_id FK
        double quantity_kg
    }
```

---

## How to View These Diagrams

### Option 1: GitHub
1. Push this file to GitHub
2. Diagrams render automatically in markdown preview

### Option 2: VS Code
1. Install "Markdown Preview Mermaid Support" extension
2. Open this file and press `Ctrl+Shift+V` (Windows) or `Cmd+Shift+V` (Mac)

### Option 3: Online Viewer
1. Go to https://mermaid.live
2. Copy any diagram code block
3. Paste and view/export as PNG/SVG

### Option 4: Mermaid CLI (Generate PNG)
```bash
# Install mermaid-cli
npm install -g @mermaid-js/mermaid-cli

# Generate PNG from this file
mmdc -i FLOW_DIAGRAMS.md -o flow_diagram_1.png
```

---

## Legend

### Colors Used
- 🔵 **Blue**: Customer actions and flows
- 🟢 **Green**: Successful processes and confirmations
- 🟡 **Yellow**: Decision points and validations
- 🟠 **Orange**: Delivery group and delivery person actions
- 🔴 **Red**: Rejection/error flows
- 🟣 **Purple**: System components and controllers

### Arrow Types
- **Solid arrows (→)**: Direct flow/process
- **Dashed arrows (-.->)**: Notifications/messages sent
- **Thick arrows (==>)**: Main data flow with timestamp
