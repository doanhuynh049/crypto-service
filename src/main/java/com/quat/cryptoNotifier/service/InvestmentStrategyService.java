package com.quat.cryptoNotifier.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service for managing common investment strategy and prompt instructions
 * This centralizes all investment preferences, allocation targets, and analysis frameworks
 * used across different AI prompt building methods.
 */
@Service
public class InvestmentStrategyService {

    private JsonNode investmentStrategy;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void loadInvestmentStrategy() {
        try {
            ClassPathResource resource = new ClassPathResource("investment-strategy.json");
            InputStream inputStream = resource.getInputStream();
            this.investmentStrategy = objectMapper.readTree(inputStream);
        } catch (IOException e) {
            System.err.println("Failed to load investment strategy configuration: " + e.getMessage());
            // Set default empty strategy to prevent null pointer exceptions
            try {
                this.investmentStrategy = objectMapper.readTree("{}");
            } catch (IOException ex) {
                throw new RuntimeException("Failed to initialize investment strategy service", ex);
            }
        }
    }

    /**
     * Get common investment context and preferences section for prompts
     */
    public String getInvestmentContextSection() {
        StringBuilder context = new StringBuilder();
        
        context.append("--- Investment Context & Preferences ---\n");
        context.append("- Timeframe: ").append(getTimeframe()).append("\n");
        context.append("- Risk Tolerance: ").append(getRiskTolerance()).append("\n");
        context.append("- Investment Philosophy: ").append(getInvestmentPhilosophy()).append("\n");
        context.append("- Exclusions: ").append(getExclusions()).append("\n\n");
        
        return context.toString();
    }

    /**
     * Get portfolio allocation targets section
     */
    public String getPortfolioTargetsSection() {
        StringBuilder targets = new StringBuilder();
        
        targets.append("--- Portfolio Allocation Targets ---\n");
        targets.append("- BTC Target Allocation: ").append(getBtcAllocation()).append("\n");
        targets.append("- ETH Target Allocation: ").append(getEthAllocation()).append("\n");
        targets.append("- Large-Cap Altcoins: ").append(getLargCapAltcoins()).append("\n");
        targets.append("- Stablecoins: ").append(getStablecoins()).append("\n");
        targets.append("- Small-Cap/Speculative: ").append(getSmallCapSpeculative()).append("\n\n");
        
        return targets.toString();
    }

    /**
     * Get sector diversification guidelines
     */
    public String getSectorDiversificationSection() {
        StringBuilder sectors = new StringBuilder();
        
        sectors.append("--- Sector Diversification Guidelines ---\n");
        sectors.append("- Layer 1 Blockchains: ").append(getLayer1Allocation()).append("\n");
        sectors.append("- Layer 2 Solutions: ").append(getLayer2Allocation()).append("\n");
        sectors.append("- DeFi Protocols: ").append(getDefiAllocation()).append("\n");
        sectors.append("- AI/ML Infrastructure: ").append(getAiAllocation()).append("\n");
        sectors.append("- Infrastructure/Oracles: ").append(getInfrastructureAllocation()).append("\n");
        sectors.append("- Real World Assets (RWA): ").append(getRwaAllocation()).append("\n");
        sectors.append("- Stablecoins/Yield: ").append(getStablecoinsYieldAllocation()).append("\n\n");
        
        return sectors.toString();
    }

    /**
     * Get risk management guidelines
     */
    public String getRiskManagementSection() {
        StringBuilder risk = new StringBuilder();
        
        risk.append("--- Risk Management Guidelines ---\n");
        risk.append("- Max single asset concentration: ").append(getMaxSingleAssetConcentration()).append("\n");
        risk.append("- Max correlated sector exposure: ").append(getMaxCorrelatedSectorExposure()).append("\n");
        risk.append("- Recommended stablecoin buffer: ").append(getRecommendedStablecoinBuffer()).append("\n");
        risk.append("- Profit-taking threshold: ").append(getProfitTakingThreshold()).append("\n");
        risk.append("- Stop-loss guideline: ").append(getStopLossGuideline()).append("\n\n");
        
        return risk.toString();
    }

    /**
     * Get analysis framework section for fundamental analysis
     */
    public String getAnalysisFrameworkSection() {
        StringBuilder framework = new StringBuilder();
        
        framework.append("--- Analysis Framework ---\n");
        framework.append("For fundamental strength assessment:\n");
        
        JsonNode fundamentalCriteria = getFundamentalCriteria();
        if (fundamentalCriteria != null && fundamentalCriteria.isArray()) {
            for (JsonNode criterion : fundamentalCriteria) {
                framework.append("- ").append(criterion.asText()).append("\n");
            }
        }
        
        framework.append("\nFor technical analysis:\n");
        JsonNode technicalFactors = getTechnicalFactors();
        if (technicalFactors != null && technicalFactors.isArray()) {
            for (JsonNode factor : technicalFactors) {
                framework.append("- ").append(factor.asText()).append("\n");
            }
        }
        
        framework.append("\nFor risk assessment:\n");
        JsonNode riskFactors = getRiskAssessmentFactors();
        if (riskFactors != null && riskFactors.isArray()) {
            for (JsonNode factor : riskFactors) {
                framework.append("- ").append(factor.asText()).append("\n");
            }
        }
        framework.append("\n");
        
        return framework.toString();
    }

    /**
     * Get decision criteria section
     */
    public String getDecisionCriteriaSection() {
        StringBuilder criteria = new StringBuilder();
        
        criteria.append("--- Decision Criteria ---\n");
        criteria.append("Buy signals:\n");
        JsonNode buySignals = getBuySignals();
        if (buySignals != null && buySignals.isArray()) {
            for (JsonNode signal : buySignals) {
                criteria.append("- ").append(signal.asText()).append("\n");
            }
        }
        
        criteria.append("\nHold signals:\n");
        JsonNode holdSignals = getHoldSignals();
        if (holdSignals != null && holdSignals.isArray()) {
            for (JsonNode signal : holdSignals) {
                criteria.append("- ").append(signal.asText()).append("\n");
            }
        }
        
        criteria.append("\nSell/Take-profit signals:\n");
        JsonNode sellSignals = getSellSignals();
        if (sellSignals != null && sellSignals.isArray()) {
            for (JsonNode signal : sellSignals) {
                criteria.append("- ").append(signal.asText()).append("\n");
            }
        }
        criteria.append("\n");
        
        return criteria.toString();
    }

    /**
     * Get investment profile section for prompts
     */
    public String getInvestmentProfileSection() {
        StringBuilder profile = new StringBuilder();
        
        profile.append("--- Investment Profile ---\n");
        profile.append("- Timeframe: ").append(getTimeframe()).append("\n");
        profile.append("- Risk Tolerance: ").append(getRiskTolerance()).append("\n");
        profile.append("- Investment Philosophy: ").append(getInvestmentPhilosophy()).append("\n");
        profile.append("- Exclusions: ").append(getExclusions()).append("\n\n");
        
        return profile.toString();
    }

    /**
     * Get complete investment strategy overview section for prompts
     */
    public String getCompleteInvestmentStrategySection() {
        StringBuilder strategy = new StringBuilder();
        strategy.append(getInvestmentProfileSection());
        strategy.append(getPortfolioTargetsSection());
        strategy.append("\nSector Allocation Guidelines:\n");
        strategy.append("- Layer 1 Blockchains: ").append(getLayer1Allocation()).append("\n");
        strategy.append("- Layer 2 Solutions: ").append(getLayer2Allocation()).append("\n");
        strategy.append("- DeFi Protocols: ").append(getDefiAllocation()).append("\n");
        strategy.append("- AI/ML Infrastructure: ").append(getAiAllocation()).append("\n");
        strategy.append("- Infrastructure/Oracles: ").append(getInfrastructureAllocation()).append("\n");
        strategy.append("\nRisk Management Rules:\n");
        strategy.append("- Max Single Asset: ").append(getMaxSingleAssetConcentration()).append("\n");
        strategy.append("- Max Sector Exposure: ").append(getMaxCorrelatedSectorExposure()).append("\n");
        strategy.append("- Stablecoin Buffer: ").append(getRecommendedStablecoinBuffer()).append("\n");
        strategy.append("- Profit-Taking Threshold: ").append(getProfitTakingThreshold()).append("\n\n");
        
        return strategy.toString();
    }

    /**
     * Get complete common instructions section that can be added to any prompt
     */
    public String getCompleteCommonInstructions() {
        StringBuilder instructions = new StringBuilder();
        
        instructions.append(getInvestmentContextSection());
        instructions.append(getPortfolioTargetsSection());
        instructions.append(getSectorDiversificationSection());
        instructions.append(getRiskManagementSection());
        instructions.append(getAnalysisFrameworkSection());
        instructions.append(getDecisionCriteriaSection());
        
        return instructions.toString();
    }

    /**
     * Get a specific section by name for flexible usage
     */
    public String getInstructionSection(String sectionName) {
        switch (sectionName.toLowerCase()) {
            case "context":
            case "investment_context":
                return getInvestmentContextSection();
            case "targets":
            case "portfolio_targets":
                return getPortfolioTargetsSection();
            case "sectors":
            case "sector_diversification":
                return getSectorDiversificationSection();
            case "risk":
            case "risk_management":
                return getRiskManagementSection();
            case "analysis":
            case "analysis_framework":
                return getAnalysisFrameworkSection();
            case "decisions":
            case "decision_criteria":
                return getDecisionCriteriaSection();
            default:
                return "";
        }
    }

    // Private helper methods to extract specific values from the JSON configuration
    private String getTimeframe() {
        return getStringValue("investmentStrategy.profile.timeframe", "6 months to 3 years");
    }

    private String getRiskTolerance() {
        return getStringValue("investmentStrategy.profile.riskTolerance", "Moderate");
    }

    private String getInvestmentPhilosophy() {
        return getStringValue("investmentStrategy.profile.investmentPhilosophy", 
                "Strong fundamentals, real-world utility, and sustainable long-term value");
    }

    private String getExclusions() {
        return getStringValue("investmentStrategy.profile.exclusions", 
                "No meme coins or highly speculative low-cap tokens");
    }

    private String getBtcAllocation() {
        return getStringValue("investmentStrategy.portfolioTargets.btcAllocation", "30%");
    }

    private String getEthAllocation() {
        return getStringValue("investmentStrategy.portfolioTargets.ethAllocation", "30%");
    }

    private String getLargCapAltcoins() {
        return getStringValue("investmentStrategy.portfolioTargets.largCapAltcoins", "10-20%");
    }

    private String getStablecoins() {
        return getStringValue("investmentStrategy.portfolioTargets.stablecoins", "20%");
    }

    private String getSmallCapSpeculative() {
        return getStringValue("investmentStrategy.portfolioTargets.smallCapSpeculative", "5-10%");
    }

    private String getLayer1Allocation() {
        return getStringValue("investmentStrategy.sectorDiversification.layer1", "45-50%");
    }

    private String getLayer2Allocation() {
        return getStringValue("investmentStrategy.sectorDiversification.layer2", "10-15%");
    }

    private String getDefiAllocation() {
        return getStringValue("investmentStrategy.sectorDiversification.defi", "15-20%");
    }

    private String getAiAllocation() {
        return getStringValue("investmentStrategy.sectorDiversification.aiMachineLearning", "8-12%");
    }

    private String getInfrastructureAllocation() {
        return getStringValue("investmentStrategy.sectorDiversification.infrastructure", "5-10%");
    }

    private String getRwaAllocation() {
        return getStringValue("investmentStrategy.sectorDiversification.realWorldAssets", "3-8%");
    }

    private String getStablecoinsYieldAllocation() {
        return getStringValue("investmentStrategy.sectorDiversification.stablecoinsYield", "15-20%");
    }

    private String getMaxSingleAssetConcentration() {
        return getStringValue("investmentStrategy.riskManagement.maxSingleAssetConcentration", "15%");
    }

    private String getMaxCorrelatedSectorExposure() {
        return getStringValue("investmentStrategy.riskManagement.maxCorrelatedSectorExposure", "30%");
    }

    private String getRecommendedStablecoinBuffer() {
        return getStringValue("investmentStrategy.riskManagement.recommendedStablecoinBuffer", "15-25%");
    }

    private String getProfitTakingThreshold() {
        return getStringValue("investmentStrategy.riskManagement.profitTakingThreshold", "50%");
    }

    private String getStopLossGuideline() {
        return getStringValue("investmentStrategy.riskManagement.stopLossGuideline", 
                "Based on individual risk tolerance and technical levels");
    }

    private JsonNode getFundamentalCriteria() {
        return getJsonNode("investmentStrategy.analysisFramework.fundamentalCriteria");
    }

    private JsonNode getTechnicalFactors() {
        return getJsonNode("investmentStrategy.analysisFramework.technicalFactors");
    }

    private JsonNode getRiskAssessmentFactors() {
        return getJsonNode("investmentStrategy.analysisFramework.riskAssessment");
    }

    private JsonNode getBuySignals() {
        return getJsonNode("investmentStrategy.decisionCriteria.buySignals");
    }

    private JsonNode getHoldSignals() {
        return getJsonNode("investmentStrategy.decisionCriteria.holdSignals");
    }

    private JsonNode getSellSignals() {
        return getJsonNode("investmentStrategy.decisionCriteria.sellTakeProfitSignals");
    }

    /**
     * Helper method to safely extract string values from nested JSON path
     */
    private String getStringValue(String jsonPath, String defaultValue) {
        try {
            String[] pathParts = jsonPath.split("\\.");
            JsonNode current = investmentStrategy;
            
            for (String part : pathParts) {
                if (current == null || !current.has(part)) {
                    return defaultValue;
                }
                current = current.get(part);
            }
            
            return current != null ? current.asText(defaultValue) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Helper method to safely extract JsonNode values from nested JSON path
     */
    private JsonNode getJsonNode(String jsonPath) {
        try {
            String[] pathParts = jsonPath.split("\\.");
            JsonNode current = investmentStrategy;
            
            for (String part : pathParts) {
                if (current == null || !current.has(part)) {
                    return null;
                }
                current = current.get(part);
            }
            
            return current;
        } catch (Exception e) {
            return null;
        }
    }

    // Public getters for direct access to configuration values
    public String getConfiguredTimeframe() {
        return getTimeframe();
    }

    public String getConfiguredRiskTolerance() {
        return getRiskTolerance();
    }

    // Add more public getters as needed for specific use cases
}
