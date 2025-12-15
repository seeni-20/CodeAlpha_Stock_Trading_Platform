import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
// --- 1. Stock Class ---
class Stock {
    private final String symbol;
    private final String name;
    private double currentPrice;

    public Stock(String symbol, String name, double initialPrice) {
        this.symbol = symbol;
        this.name = name;
        this.currentPrice = initialPrice;
    }
    
    // Used for simple persistence loading
    public void setCurrentPrice(double price) {
        this.currentPrice = price;
    }
    
    public void updatePrice(double changePercent) {
        this.currentPrice *= (1 + changePercent);
        this.currentPrice = Math.round(this.currentPrice * 100.0) / 100.0;
        if (this.currentPrice < 0.01) this.currentPrice = 0.01;
    }
    
    public String getSymbol() { return symbol; }
    public String getName() { return name; }
    public double getCurrentPrice() { return currentPrice; }
    
    @Override
    public String toString() {
        return String.format("%s (%s): $%.2f", symbol, name, currentPrice);
    }
}

// --- 2. Portfolio Class ---
class Portfolio {
    // Key: Stock Symbol, Value: Quantity
    private final Map<String, Integer> holdings;
    // Map to track cost basis (for simpler ROI calculation)
    private final Map<String, Double> costBasis; 
    
    public Portfolio() {
        this.holdings = new HashMap<>();
        this.costBasis = new HashMap<>();
    }
    
    // Called when buying
    public void updateHolding(String symbol, int quantity, double price) {
        int oldQuantity = holdings.getOrDefault(symbol, 0);
        double oldCostBasis = costBasis.getOrDefault(symbol, 0.0);
        double newCost = quantity * price;

        int newTotalQuantity = oldQuantity + quantity;
        double newTotalCost = oldCostBasis * oldQuantity + newCost;
        
        holdings.put(symbol, newTotalQuantity);
        
        // Calculate the new average cost basis
        if (newTotalQuantity > 0) {
            costBasis.put(symbol, newTotalCost / newTotalQuantity);
        } else {
            costBasis.remove(symbol);
        }
    }

    public double getMarketValue(Map<String, Stock> marketStocks) {
        double totalValue = 0.0;
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            String symbol = entry.getKey();
            int quantity = entry.getValue();
            if (marketStocks.containsKey(symbol)) {
                totalValue += quantity * marketStocks.get(symbol).getCurrentPrice();
            }
        }
        return Math.round(totalValue * 100.0) / 100.0;
    }

    // New, simpler ROI calculation for a single stock
    public String getProfitLoss(String symbol, double currentPrice) {
        if (!costBasis.containsKey(symbol)) return "N/A";
        
        double basis = costBasis.get(symbol);
        double gain = currentPrice - basis;
        double percentage = (gain / basis) * 100.0;
        
        return String.format("$%.2f (%.2f%%)", gain, percentage);
    }

    public Map<String, Integer> getHoldings() { return holdings; }
    public Map<String, Double> getCostBasis() { return costBasis; }
}

// --- 3. User Class ---
class User {
    private final int userId;
    private final String username;
    private double cashBalance;
    private final Portfolio portfolio;
    
    public User(int userId, String username, double initialBalance) {
        this.userId = userId;
        this.username = username;
        this.cashBalance = initialBalance;
        this.portfolio = new Portfolio();
    }
    
    public void setCashBalance(double balance) { 
        this.cashBalance = balance; 
    }
    
    public void deposit(double amount) {
        this.cashBalance += amount;
        this.cashBalance = Math.round(this.cashBalance * 100.0) / 100.0;
    }

    public boolean withdraw(double amount) {
        if (amount > 0 && this.cashBalance >= amount) {
            this.cashBalance -= amount;
            this.cashBalance = Math.round(this.cashBalance * 100.0) / 100.0;
            return true;
        }
        return false;
    }
    
    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public double getCashBalance() { return cashBalance; }
    public Portfolio getPortfolio() { return portfolio; }
    
    @Override
    public String toString() {
        return String.format("User: %s | Cash Balance: $%.2f", username, cashBalance);
    }
}

// --- 4. TradingPlatform Class (The Engine) ---
public class TradingSimulator {
    private final Map<String, Stock> marketStocks;
    private final User trader;
    private final Random random;
    private static final String DATA_FILE = "simulator_save.txt";

    public TradingSimulator() {
        this.marketStocks = new HashMap<>();
        this.random = new Random();
        
        // Initialize default market data
        marketStocks.put("AAPL", new Stock("AAPL", "Apple Inc.", 170.50));
        marketStocks.put("GOOG", new Stock("GOOG", "Alphabet Inc.", 1500.75));
        marketStocks.put("MSFT", new Stock("MSFT", "Microsoft Corp.", 300.20));

        // Attempt to load user data or start fresh
        trader = new User(101, "CodAlpha Trader", 10000.00);
        loadData();
    }

    public void displayMarketData() {
        System.out.println("\n--- Current Market Prices ---");
        System.out.printf("| %-5s | %-20s | %-12s |\n", "Symbol", "Name", "Price");
        System.out.println("---------------------------------------------");
        for (Stock stock : marketStocks.values()) {
            System.out.printf("| %-5s | %-20s | $%,10.2f |\n", 
                               stock.getSymbol(), 
                               stock.getName(), 
                               stock.getCurrentPrice());
        }
        System.out.println("---------------------------------------------");
    }

    public void simulateMarketChange() {
        System.out.println("\n--- Market Fluctuation Simulated ---");
        for (Stock stock : marketStocks.values()) {
            double change = random.nextDouble() * 0.04 - 0.02; 
            stock.updatePrice(change);
            System.out.printf("%s: New Price $%.2f (Change: %.2f%%)\n", 
                              stock.getSymbol(), 
                              stock.getCurrentPrice(), 
                              change * 100);
        }
    }
    
    public void executeBuy(String symbol, int quantity) {
        Stock stock = marketStocks.get(symbol);
        if (stock == null) {
            System.out.println("Error: Invalid Stock Symbol.");
            return;
        }

        double price = stock.getCurrentPrice();
        double cost = Math.round(price * quantity * 100.0) / 100.0;

        if (trader.getCashBalance() >= cost) {
            trader.withdraw(cost);
            trader.getPortfolio().updateHolding(symbol, quantity, price);
            
            System.out.printf("SUCCESS: Bought %d shares of %s at $%.2f for a total of $%.2f.\n",
                               quantity, symbol, price, cost);
        } else {
            System.out.printf("FAILURE: Insufficient funds. Need $%.2f, but only have $%.2f.\n",
                               cost, trader.getCashBalance());
        }
    }

    public void executeSell(String symbol, int quantity) {
        Stock stock = marketStocks.get(symbol);
        int currentHolding = trader.getPortfolio().getHoldings().getOrDefault(symbol, 0);

        if (stock == null) {
             System.out.println("Error: Invalid Stock Symbol.");
            return;
        }

        if (currentHolding >= quantity) {
            double price = stock.getCurrentPrice();
            double proceeds = Math.round(price * quantity * 100.0) / 100.0;
            
            trader.deposit(proceeds);
            // Selling is treated as buying a negative quantity for portfolio update
            trader.getPortfolio().updateHolding(symbol, -quantity, price); 
            
            System.out.printf("SUCCESS: Sold %d shares of %s at $%.2f for a total of $%.2f.\n",
                               quantity, symbol, price, proceeds);
        } else {
            System.out.printf("FAILURE: Insufficient holdings. You only own %d shares of %s.\n",
                               currentHolding, symbol);
        }
    }
    
    // --- Portfolio Tracking with P/L ---
    public void trackPortfolio() {
        System.out.printf("\n--- Portfolio Performance for %s ---\n", trader.getUsername());
        System.out.printf("Cash Balance: $%,.2f\n", trader.getCashBalance());
        
        Map<String, Integer> holdings = trader.getPortfolio().getHoldings();
        if (holdings.isEmpty()) {
            System.out.println("No stocks currently held.");
        } else {
            double totalHoldingsValue = 0.0;
            System.out.printf("| %-5s | %-5s | %-12s | %-12s | %-20s |\n", "Sym", "Qty", "Price", "Value", "P/L vs Cost Basis");
            System.out.println("---------------------------------------------------------------------");

            for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
                String symbol = entry.getKey();
                int quantity = entry.getValue();
                Stock stock = marketStocks.get(symbol);
                double value = Math.round(quantity * stock.getCurrentPrice() * 100.0) / 100.0;
                totalHoldingsValue += value;
                
                System.out.printf("| %-5s | %-5d | $%,10.2f | $%,10.2f | %-20s |\n", 
                                  symbol, 
                                  quantity, 
                                  stock.getCurrentPrice(), 
                                  value,
                                  trader.getPortfolio().getProfitLoss(symbol, stock.getCurrentPrice()));
            }
            System.out.printf("\nTOTAL Holdings Market Value: $%,.2f\n", totalHoldingsValue);
        }
        System.out.println("------------------------------------------------------");
    }

    // --- Simple File I/O Persistence ---
    private void saveData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            // Line 1: Cash Balance
            writer.write(String.valueOf(trader.getCashBalance()));
            writer.newLine();
            
            // Line 2: Holdings (Symbol:Quantity:CostBasis,Symbol:Quantity:CostBasis)
            StringBuilder holdingsLine = new StringBuilder();
            for (Map.Entry<String, Integer> entry : trader.getPortfolio().getHoldings().entrySet()) {
                String symbol = entry.getKey();
                holdingsLine.append(symbol).append(":")
                            .append(entry.getValue()).append(":")
                            .append(trader.getPortfolio().getCostBasis().get(symbol)).append(",");
            }
            writer.write(holdingsLine.toString());
            writer.newLine();
            
            // Line 3: Stock Prices (Symbol:Price,Symbol:Price)
            StringBuilder priceLine = new StringBuilder();
            for (Stock stock : marketStocks.values()) {
                priceLine.append(stock.getSymbol()).append(":")
                         .append(stock.getCurrentPrice()).append(",");
            }
            writer.write(priceLine.toString());
            
            System.out.println("\n[Data saved successfully to " + DATA_FILE + "]");
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    private void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            System.out.println("[No saved data found. Starting fresh.]");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            // Line 1: Cash Balance
            String cashLine = reader.readLine();
            if (cashLine != null) {
                trader.setCashBalance(Double.parseDouble(cashLine));
            }
            
            // Line 2: Holdings
            String holdingsLine = reader.readLine();
            if (holdingsLine != null && !holdingsLine.isEmpty()) {
                String[] holdingsArr = holdingsLine.split(",");
                for (String holding : holdingsArr) {
                    String[] parts = holding.split(":");
                    if (parts.length == 3) {
                        String symbol = parts[0];
                        int quantity = Integer.parseInt(parts[1]);
                        double costBasis = Double.parseDouble(parts[2]);
                        
                        trader.getPortfolio().getHoldings().put(symbol, quantity);
                        trader.getPortfolio().getCostBasis().put(symbol, costBasis);
                    }
                }
            }

            // Line 3: Stock Prices
            String priceLine = reader.readLine();
            if (priceLine != null && !priceLine.isEmpty()) {
                String[] priceArr = priceLine.split(",");
                for (String priceData : priceArr) {
                    String[] parts = priceData.split(":");
                    if (parts.length == 2) {
                        String symbol = parts[0];
                        double price = Double.parseDouble(parts[1]);
                        
                        // Update the current market prices with saved data
                        if (marketStocks.containsKey(symbol)) {
                            marketStocks.get(symbol).setCurrentPrice(price);
                        }
                    }
                }
            }
            System.out.println("[Data loaded successfully from " + DATA_FILE + "]");

        } catch (IOException | NumberFormatException e) {
            System.err.println("Error loading data: " + e.getMessage());
            // If load fails, keep the initial fresh state
        }
    }
    
    // Main execution method
    public static void main(String[] args) {
        TradingSimulator platform = new TradingSimulator();
        
        System.out.println("\n=============================================");
        System.out.println("      SIMPLIFIED STOCK SIMULATOR START     ");
        System.out.println("=============================================");
        System.out.println(platform.trader);

        // --- SIMULATION ---
        platform.displayMarketData();
        
        System.out.println("\n--- Trading Session 1 ---");
        platform.executeBuy("AAPL", 5);
        platform.executeBuy("GOOG", 1);
        
        platform.trackPortfolio();
        
        // Time passes and market moves
        platform.simulateMarketChange();
        
        System.out.println("\n--- Trading Session 2 ---");
        platform.executeSell("AAPL", 2);
        
        platform.trackPortfolio();
        
        platform.saveData(); 
        
        System.out.println("\nSimulation complete. Rerun the file to see persistence in action.");
    }
}