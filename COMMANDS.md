# Crypto Advisory Service - Command Reference

A quick reference guide for all available curl commands to interact with the Crypto Advisory Service.

## Service Health Check

```bash
# Check if the service is running
curl http://localhost:8080/api/health
```

## Portfolio Management Commands

### Holdings Reordering

```bash
# Reorder holdings by total average cost (highest first) - DEFAULT
curl -X POST http://localhost:8080/api/reorder-holdings

# Reorder holdings by total average cost (highest first) - EXPLICIT
curl -X POST "http://localhost:8080/api/reorder-holdings?sortBy=total_avg_cost&order=desc"

# Reorder holdings by total average cost (lowest first)
curl -X POST "http://localhost:8080/api/reorder-holdings?sortBy=total_avg_cost&order=asc"

# Reorder holdings by total current value (highest first) - NEW!
curl -X POST "http://localhost:8080/api/reorder-holdings?sortBy=total_current_value&order=desc"

# Reorder holdings by total current value (lowest first) - NEW!
curl -X POST "http://localhost:8080/api/reorder-holdings?sortBy=total_current_value&order=asc"
```

### Holdings Information

```bash
# Get holdings summary with total average cost calculations
curl http://localhost:8080/api/holdings-summary
```

### XLSX Export

```bash
# Export holdings to Excel file sorted by current value (default)
curl -o "portfolio.xlsx" "http://localhost:8080/api/export-holdings-xlsx"

# Export holdings sorted by total average cost
curl -o "portfolio.xlsx" "http://localhost:8080/api/export-holdings-xlsx?sortBy=total_avg_cost&order=desc"

# Export holdings sorted by current value (ascending)
curl -o "portfolio.xlsx" "http://localhost:8080/api/export-holdings-xlsx?sortBy=total_current_value&order=asc"

# Export with custom filename
curl -o "my-portfolio-$(date +%Y%m%d).xlsx" "http://localhost:8080/api/export-holdings-xlsx"
```

## Advisory Analysis Commands

### Full Advisory Analysis

```bash
# Trigger complete advisory analysis (all modules)
curl http://localhost:8080/api/trigger-advisory

# Risk and opportunity analysis only
curl http://localhost:8080/api/test-risk-analysis
```

### Portfolio Analysis

```bash
# Portfolio health check analysis
curl http://localhost:8080/api/test-portfolio-health

# Portfolio optimization recommendations
curl http://localhost:8080/api/test-portfolio-optimization

# Investment strategy and target review
curl http://localhost:8080/api/test-strategy-review
```

### Individual Cryptocurrency Analysis

```bash
# ETH investment analysis (default parameters)
curl http://localhost:8080/api/test-investment-analysis

# ETH specific detailed analysis
curl http://localhost:8080/api/test-eth-investment-analysis

# Custom cryptocurrency analysis
curl "http://localhost:8080/api/test-investment-analysis?symbol=BTC&name=Bitcoin"
curl "http://localhost:8080/api/test-investment-analysis?symbol=SOL&name=Solana"
curl "http://localhost:8080/api/test-investment-analysis?symbol=ADA&name=Cardano"
curl "http://localhost:8080/api/test-investment-analysis?symbol=DOT&name=Polkadot"
curl "http://localhost:8080/api/test-investment-analysis?symbol=AVAX&name=Avalanche"
curl "http://localhost:8080/api/test-investment-analysis?symbol=LINK&name=Chainlink"
curl "http://localhost:8080/api/test-investment-analysis?symbol=UNI&name=Uniswap"
```

## Command Categories by Use Case

### Daily Portfolio Monitoring

```bash
# Morning portfolio check routine
curl http://localhost:8080/api/health
curl http://localhost:8080/api/holdings-summary
curl http://localhost:8080/api/test-portfolio-health
```

### Weekly Portfolio Review

```bash
# Comprehensive weekly analysis
curl http://localhost:8080/api/test-portfolio-health
curl http://localhost:8080/api/test-portfolio-optimization
curl http://localhost:8080/api/test-strategy-review
curl http://localhost:8080/api/test-risk-analysis
```

### Portfolio Rebalancing Session

```bash
# Before rebalancing
curl http://localhost:8080/api/holdings-summary
curl http://localhost:8080/api/test-portfolio-optimization

# Organize holdings by current market value (NEW!)
curl -X POST "http://localhost:8080/api/reorder-holdings?sortBy=total_current_value&order=desc"

# Organize holdings by investment size (historical cost)
curl -X POST "http://localhost:8080/api/reorder-holdings?sortBy=total_avg_cost&order=desc"

# Export current portfolio to Excel for analysis
curl -o "pre-rebalance-$(date +%Y%m%d).xlsx" "http://localhost:8080/api/export-holdings-xlsx"

# After rebalancing
curl http://localhost:8080/api/holdings-summary
```

### Excel Export and Analysis

```bash
# Export portfolio sorted by current value for spreadsheet analysis
curl -o "portfolio-current-value.xlsx" "http://localhost:8080/api/export-holdings-xlsx?sortBy=total_current_value"

# Export portfolio sorted by investment cost for tax reporting
curl -o "portfolio-cost-basis.xlsx" "http://localhost:8080/api/export-holdings-xlsx?sortBy=total_avg_cost"

# Export smallest positions first to identify rebalancing candidates
curl -o "small-positions.xlsx" "http://localhost:8080/api/export-holdings-xlsx?order=asc"

# Daily export with timestamp
curl -o "daily-portfolio-$(date +%Y-%m-%d).xlsx" "http://localhost:8080/api/export-holdings-xlsx"
```

## HTTP Methods Reference

| Endpoint | Method | Parameters | Purpose |
|----------|--------|------------|---------|
| `/api/health` | GET | None | Service health check |
| `/api/holdings-summary` | GET | None | Holdings overview |
| `/api/reorder-holdings` | POST | `sortBy` (total_avg_cost/total_current_value), `order` (desc/asc) | Reorder holdings file |
| `/api/export-holdings-xlsx` | GET | `sortBy` (total_avg_cost/total_current_value), `order` (desc/asc) | Export portfolio to Excel |
| `/api/trigger-advisory` | GET | None | Full advisory analysis |
| `/api/test-risk-analysis` | GET | None | Risk & opportunity analysis |
| `/api/test-portfolio-health` | GET | None | Portfolio health check |
| `/api/test-portfolio-optimization` | GET | None | Portfolio optimization |
| `/api/test-strategy-review` | GET | None | Strategy & target review |
| `/api/test-investment-analysis` | GET | `symbol`, `name` | Investment analysis |
| `/api/test-eth-investment-analysis` | GET | None | ETH specific analysis |

## Error Handling Examples

```bash
# Check for 405 Method Not Allowed (wrong HTTP method)
curl http://localhost:8080/api/reorder-holdings  # This will fail - missing POST

# Correct way
curl -X POST http://localhost:8080/api/reorder-holdings  # This will work

# Check response status
curl -w "%{http_code}" -o /dev/null -s http://localhost:8080/api/health
```

## Automation Scripts

### Bash Script for Daily Check

```bash
#!/bin/bash
echo "=== Daily Crypto Portfolio Check ==="
curl http://localhost:8080/api/health
echo -e "\n=== Holdings Summary ==="
curl http://localhost:8080/api/holdings-summary
echo -e "\n=== Portfolio Health ==="
curl http://localhost:8080/api/test-portfolio-health
```

### Cron Job Examples

```bash
# Add to crontab with: crontab -e

# Daily portfolio health check at 8 AM
0 8 * * * curl http://localhost:8080/api/test-portfolio-health

# Weekly full advisory every Sunday at 9 AM
0 9 * * 0 curl http://localhost:8080/api/trigger-advisory

# Reorder holdings by value every Monday at 7 AM
0 7 * * 1 curl -X POST "http://localhost:8080/api/reorder-holdings?order=desc"
```

## Troubleshooting Commands

```bash
# Test service connectivity
curl -I http://localhost:8080/api/health

# Verbose output for debugging
curl -v http://localhost:8080/api/health

# Save response to file
curl http://localhost:8080/api/holdings-summary > holdings-summary.json

# Test with timeout
curl --max-time 30 http://localhost:8080/api/test-portfolio-health
```

## Expected Response Examples

### Successful Health Check
```
Crypto Advisory Service is running
```

### Successful Holdings Reorder
```
Holdings successfully reordered by total average cost (DESC). 19 holdings processed.
```

### Holdings Summary Response
```json
{
  "totalHoldings": 19,
  "totalAverageCost": 450.25,
  "holdings": [...]
}
```

## Notes

- **Service Must Be Running**: Ensure the service is started before running commands
- **Email Configuration**: Analysis commands send results via email (check your email)
- **File Changes**: The reorder command modifies `holdings.json` file
- **Rate Limiting**: External API calls may have rate limits
- **Network**: Ensure localhost:8080 is accessible

---

**Quick Start**: Begin with `curl http://localhost:8080/api/health` to verify service status
**Most Used**: `curl -X POST http://localhost:8080/api/reorder-holdings` and `curl http://localhost:8080/api/holdings-summary`
