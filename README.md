# Crypto Advisory Notifier

A comprehensive Java Spring Boot application that provides automated daily crypto portfolio analysis with AI-powered risk assessment and opportunity identification.

## Features

- **Dual-Phase AI Analysis**: Portfolio overview + detailed risk & opportunity analysis
- **Smart Email System**: Two structured emails with comprehensive market insights
- **Advanced Portfolio Analytics**: Sector diversification, correlation analysis, risk scoring
- **Real-time Market Data**: Integration with CoinGecko API for 13+ cryptocurrencies
- **Risk Assessment Engine**: Automated evaluation of portfolio balance and exposure
- **Investment Recommendations**: AI-driven suggestions for portfolio optimization
- **Daily Automation**: Scheduled execution at 7:30 AM Asia/Ho_Chi_Minh timezone
- **Comprehensive Logging**: Daily snapshots with detailed analysis results

## New Portfolio Structure

The application now uses an enhanced portfolio structure with detailed crypto information:

### Updated Holdings Format
```json
{
  "portfolio": {
    "name": "Main Crypto Portfolio",
    "cryptos": [
      {
        "id": "bitcoin",
        "symbol": "BTC", 
        "name": "Bitcoin",
        "holdings": 0.25,
        "averagePrice": 45000.0,
        "initialValue": 11250.0,
        "expectedEntry": 43000.0,
        "expectedDeepEntry": 75000.0,
        "targetPrice3Month": 65000.0,
        "targetPriceLongTerm": 100000.0
      }
    ]
  }
}
```

## Email Analysis Types

### 1. Portfolio Overview Email 📈
- **Risk Assessment**: Overall portfolio risk profile and exposure analysis
- **Allocation Analysis**: Weight distribution and balance recommendations  
- **Opportunity Identification**: Sectors with growth potential
- **Missing Assets**: Suggestions for diversification improvements
- **Action Recommendations**: Strategic portfolio adjustments

### 2. Risk & Opportunity Analysis Email 🎯
- **Individual Coin Analysis**: Detailed assessment for each holding
  - Short-term vs long-term market outlook
  - Risk factors and volatility considerations
  - Upside opportunities and growth catalysts
  - Buy/Hold/Sell recommendations with rationale
- **Portfolio-Level Analysis**:
  - Sector diversification breakdown (Layer 1, DeFi, AI, etc.)
  - Correlation risks and concentration concerns
  - Diversification scoring (Poor/Fair/Good/Excellent)
  - Strategic adjustment recommendations with priority levels
- **Executive Summary**: High-level insights and key action items

## Configuration

### Application Properties Setup

1. Copy the template configuration file:
   ```bash
   cp src/main/resources/application.properties.template src/main/resources/application.properties
   ```

2. Update `src/main/resources/application.properties` with your actual configuration:

```properties
# LLM Provider
app.llm-provider=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent
app.llm-api-key=YOUR_ACTUAL_GEMINI_API_KEY

# Email Settings
app.smtp-user=your-actual-email@gmail.com
app.smtp-pass=your-actual-app-password
app.mail-from=your-actual-email@gmail.com
app.mail-to=recipient@gmail.com
```

**Note**: Never commit your actual `application.properties` file with sensitive credentials to version control.

## Holdings Configuration

Update `src/main/resources/holdings.json` with your crypto positions using the new enhanced format:

```json
{
  "portfolio": {
    "name": "Main Crypto Portfolio",
    "cryptos": [
      {
        "id": "bitcoin",
        "symbol": "BTC",
        "name": "Bitcoin", 
        "holdings": 0.25,
        "averagePrice": 45000.0,
        "initialValue": 11250.0,
        "expectedEntry": 43000.0,
        "expectedDeepEntry": 75000.0,
        "targetPrice3Month": 65000.0,
        "targetPriceLongTerm": 100000.0
      },
      {
        "id": "ethereum",
        "symbol": "ETH",
        "name": "Ethereum",
        "holdings": 5.0,
        "averagePrice": 3100.0,
        "initialValue": 15500.0,
        "expectedEntry": 2900.0,
        "expectedDeepEntry": 4200.0,
        "targetPrice3Month": 3800.0,
        "targetPriceLongTerm": 6000.0
      }
    ]
  }
}
```

### Supported Cryptocurrencies (Pre-configured)
- **BTC** (Bitcoin) - `bitcoin`
- **ETH** (Ethereum) - `ethereum` 
- **ADA** (Cardano) - `cardano`
- **DOT** (Polkadot) - `polkadot`
- **SOL** (Solana) - `solana`
- **MATIC** (Polygon) - `matic-network`
- **LINK** (Chainlink) - `chainlink`
- **UNI** (Uniswap) - `uniswap`
- **AVAX** (Avalanche) - `avalanche-2`
- **ATOM** (Cosmos) - `cosmos`
- **FET** (Fetch.ai) - `fetch-ai`
- **OCEAN** (Ocean Protocol) - `ocean-protocol`
- **AGIX** (SingularityNET) - `singularitynet`

## Running the Application

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build and Run

```bash
# Build the application
mvn clean compile

# Run the application
mvn spring-boot:run

cd /home/quathd_t/workspace/data_workspace/crypto-service && mvn spring-boot:run
netstat -tulpn | grep :8080
# Alternative: Build JAR and run
mvn clean package
java -jar target/crypto-notifier-0.0.1-SNAPSHOT.jar
```

### Manual Testing

The application provides REST endpoints for manual testing:

- **Health Check**: `GET http://localhost:8080/api/health`
- **Full Advisory Run**: `GET http://localhost:8080/api/trigger-advisory`
- **Risk Analysis Test**: `GET http://localhost:8080/api/test-risk-analysis`

```bash
# Test endpoints
curl http://localhost:8080/api/health
curl http://localhost:8080/api/trigger-advisory
curl http://localhost:8080/api/test-risk-analysis
```
## Project Structure

```
src/main/java/com/quat/cryptoNotifier/
├── config/          # Configuration classes (API keys, email setup)
├── model/           # Enhanced data models
│   ├── Holding.java              # Individual crypto holding with targets
│   ├── Portfolio.java            # Portfolio container model
│   ├── Holdings.java             # Holdings wrapper with portfolio
│   ├── Advisory.java             # AI advisory recommendations
│   └── MarketData.java           # Market data and technical indicators
├── service/         # Business logic services
│   ├── DataProviderService.java      # CoinGecko API integration (13+ cryptos)
│   ├── AdvisoryEngineService.java    # Dual-phase AI analysis engine
│   ├── EmailService.java             # Multi-template email system
│   └── SchedulerService.java         # Orchestration and workflow management
├── util/            # Utility classes (technical indicators)
└── controller/      # REST endpoints for manual testing

src/main/resources/
├── templates/       # Enhanced Thymeleaf email templates
│   ├── portfolio-overview.html       # Portfolio analysis email
│   ├── risk-opportunity-analysis.html # Detailed risk analysis email
│   ├── token-advisory.html           # Individual token email (legacy)
│   └── combined-advisory.html        # Combined email template (legacy)
├── holdings.json    # Enhanced portfolio configuration
└── application.properties            # Application configuration
```

## How It Works

### Dual-Phase Analysis Workflow

1. **Initialization**: Loads portfolio from `holdings.json` with enhanced crypto data
2. **Phase 1 - Portfolio Overview**:
   - Generates high-level portfolio analysis using AI
   - Assesses risk profile and allocation balance
   - Identifies opportunities and missing sectors
   - Sends portfolio overview email
3. **Phase 2 - Risk & Opportunity Analysis**:
   - Conducts detailed individual coin analysis
   - Evaluates sector diversification and correlation risks
   - Provides strategic recommendations with priority levels
   - Sends comprehensive risk analysis email
4. **Logging**: Saves detailed snapshots to `logs/advisory-YYYY-MM-DD.log`

### AI Analysis Features

- **Portfolio Risk Scoring**: Automated assessment of diversification quality
- **Sector Analysis**: Layer 1, Layer 2, DeFi, AI/ML exposure evaluation
- **Correlation Risk Detection**: Identifies over-concentration in similar assets
- **Strategic Recommendations**: Prioritized action items (High/Medium/Low priority)
- **Market Outlook Analysis**: Short-term vs long-term perspectives for each holding

## Email Features

### 1. Portfolio Overview Email 📈
**Template**: `portfolio-overview.html`
- **Risk Assessment**: Portfolio-wide risk profile evaluation
- **Allocation Recommendations**: Strategic weight adjustments for each holding
- **Opportunity Sectors**: High-potential areas for investment (AI, DeFi, Web3, etc.)
- **Missing Assets**: Diversification gaps and suggested additions
- **Executive Summary**: Key insights and recommended actions

### 2. Risk & Opportunity Analysis Email 🎯
**Template**: `risk-opportunity-analysis.html`
- **Executive Summary**: Overall portfolio health and key recommendations
- **Portfolio Analysis**:
  - Sector diversification breakdown with exposure analysis
  - Risk assessment cards with color-coded indicators
  - Correlation and concentration risk evaluation
  - Diversification scoring system
- **Strategic Recommendations**: Prioritized action items with rationale
- **Individual Coin Analysis**: Detailed assessment for each holding
  - Market outlook (short-term vs long-term)
  - Risk factors and monitoring points
  - Upside opportunities and catalysts
  - Buy/Hold/Sell recommendations with supporting rationale
- **Missing Investment Opportunities**: Sectors to consider for improved diversification

### Email Template Features
- **Responsive Design**: Optimized for desktop and mobile viewing
- **Color-Coded Indicators**: Visual risk levels and recommendation priorities
- **Structured Layout**: Grid-based design for easy information consumption
- **Modern Styling**: Professional appearance with gradient backgrounds
- **Interactive Elements**: Priority badges and action recommendations

## Supported Cryptocurrencies

The application includes pre-configured support for 13 major cryptocurrencies with automatic CoinGecko ID mapping. Each crypto includes comprehensive data for AI analysis:

| Symbol | Name | CoinGecko ID | Sector |
|--------|------|--------------|--------|
| BTC | Bitcoin | bitcoin | Layer 1 |
| ETH | Ethereum | ethereum | Layer 1 |
| ADA | Cardano | cardano | Layer 1 |
| DOT | Polkadot | polkadot | Interoperability |
| SOL | Solana | solana | Layer 1 |
| MATIC | Polygon | matic-network | Layer 2 |
| LINK | Chainlink | chainlink | Oracle |
| UNI | Uniswap | uniswap | DeFi |
| AVAX | Avalanche | avalanche-2 | Layer 1 |
| ATOM | Cosmos | cosmos | Interoperability |
| FET | Fetch.ai | fetch-ai | AI/ML |
| OCEAN | Ocean Protocol | ocean-protocol | AI/ML |
| AGIX | SingularityNET | singularitynet | AI/ML |

Additional cryptocurrencies can be added by updating the ID mapping in `DataProviderService.java`.

## Security Notes

- Store API keys securely (consider using environment variables in production)
- Use Gmail App Passwords for email authentication
- The application logs may contain sensitive data - secure log files appropriately

## Disclaimer

This application provides automated analysis for informational purposes only. It is not financial advice. Always conduct your own research before making investment decisions.

## Future Enhancements

- **Enhanced AI Models**: Integration with additional LLM providers
- **Real-time Alerts**: Price threshold and technical indicator notifications  
- **Web Dashboard**: Interactive portfolio management interface
- **Database Persistence**: PostgreSQL integration for historical analysis
- **Advanced Technical Analysis**: Additional indicators and chart patterns
- **Multi-exchange Support**: Binance, Coinbase Pro integration
- **Portfolio Simulation**: Backtesting and scenario analysis
- **Mobile App**: React Native companion application
- **Social Sentiment Analysis**: Twitter and Reddit sentiment integration
- **DeFi Integration**: Yield farming and staking opportunity analysis

## Recent Updates (v2.0)

### ✅ Completed Features
- **Enhanced Data Model**: Comprehensive crypto holding structure with targets
- **Dual-Phase AI Analysis**: Portfolio overview + detailed risk assessment
- **Advanced Email Templates**: Professional HTML templates with responsive design
- **Risk Scoring System**: Automated portfolio diversification evaluation
- **Sector Analysis**: Layer 1, DeFi, AI exposure breakdown
- **Strategic Recommendations**: Prioritized action items with rationale
- **Improved Error Handling**: Robust JSON parsing and API failure recovery
- **GitHub-Ready Structure**: Comprehensive `.gitignore` and documentation
