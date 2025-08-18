# Crypto Advisory Notifier

A Java Spring Boot application that provides automated daily crypto portfolio analysis and advisory emails.

## Features

- **Daily Automated Analysis**: Runs at 7:30 AM Asia/Ho_Chi_Minh timezone
- **Market Data Integration**: Fetches real-time crypto prices from CoinGecko API
- **Technical Analysis**: Calculates RSI, MACD, SMA indicators
- **AI-Powered Advisory**: Uses Google Gemini API for investment recommendations
- **Email Notifications**: Sends detailed HTML emails for each token and portfolio overview
- **Logging**: Maintains daily snapshots of analysis results

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

Update `src/main/resources/holdings.json` with your crypto positions:

```json
{
  "positions": [
    { "symbol": "BTC", "amount": 0.3, "avgPrice": 62000, "targetPrice": 75000, "maxDrawdownPct": -15 },
    { "symbol": "ETH", "amount": 5, "avgPrice": 3100, "targetPrice": 4200, "maxDrawdownPct": -12 }
  ]
}
```

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

- Health Check: `GET http://localhost:8080/api/health`
- Trigger Advisory: `GET http://localhost:8080/api/trigger-advisory`

curl http://localhost:8080/api/health
curl http://localhost:8080/api/trigger-advisory
## Project Structure

```
src/main/java/com/quat/cryptoNotifier/
├── config/          # Configuration classes (API keys, email setup)
├── model/           # Data models (Holding, Advisory, MarketData)
├── service/         # Business logic services
│   ├── DataProviderService.java      # CoinGecko API integration
│   ├── AdvisoryEngineService.java    # AI analysis and recommendations
│   ├── EmailService.java             # Email sending with templates
│   └── SchedulerService.java         # Daily scheduler and orchestration
├── util/            # Utility classes (technical indicators)
└── controller/      # REST endpoints for manual testing

src/main/resources/
├── templates/       # Thymeleaf email templates
│   ├── token-advisory.html           # Individual token email template
│   └── portfolio-overview.html       # Portfolio summary email template
├── holdings.json    # Your crypto positions configuration
└── application.properties            # Application configuration
```

## How It Works

1. **Scheduled Execution**: Daily at 7:30 AM (configurable)
2. **Data Collection**: Fetches current prices and historical data from CoinGecko
3. **Technical Analysis**: Calculates RSI, MACD, SMA20/50/200 indicators
4. **AI Analysis**: Sends structured data to Gemini API for recommendations
5. **Email Delivery**: Sends individual token advisories and portfolio overview
6. **Logging**: Saves daily snapshots to `logs/advisory-YYYY-MM-DD.log`

## Email Features

### Individual Token Advisory
- Current position details (holdings, average price, target price)
- Performance metrics (P&L, percentage gains/losses)
- Technical indicators with visual formatting
- AI-generated recommendations with rationale
- Key support/resistance levels
- Risk considerations

### Portfolio Overview
- Total portfolio value and P&L summary
- Individual holdings performance table
- Technical indicators summary
- Consolidated AI recommendations

## Supported Cryptocurrencies

The application supports major cryptocurrencies with automatic CoinGecko ID mapping:
- BTC (Bitcoin)
- ETH (Ethereum)
- ADA (Cardano)
- DOT (Polkadot)
- SOL (Solana)
- MATIC (Polygon)
- LINK (Chainlink)
- UNI (Uniswap)
- AVAX (Avalanche)
- ATOM (Cosmos)

Additional tokens can be added by updating the `getCoinGeckoId()` method in `DataProviderService`.

## Security Notes

- Store API keys securely (consider using environment variables in production)
- Use Gmail App Passwords for email authentication
- The application logs may contain sensitive data - secure log files appropriately

## Disclaimer

This application provides automated analysis for informational purposes only. It is not financial advice. Always conduct your own research before making investment decisions.

## Future Enhancements

- Database persistence (SQLite/PostgreSQL)
- Web dashboard interface
- Additional technical indicators
- Multiple exchange support
- Mobile push notifications
- Portfolio rebalancing suggestions
