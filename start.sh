#!/bin/bash

# Crypto Advisory Notifier Startup Script

echo "🚀 Starting Crypto Advisory Notifier..."

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | grep -oP 'version "([0-9]+)' | grep -oP '[0-9]+')
if [ "$JAVA_VERSION" -lt "17" ]; then
    echo "❌ Java 17 or higher is required. Current version: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java version check passed"

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven 3.6 or higher."
    exit 1
fi

echo "✅ Maven check passed"

# Create logs directory if it doesn't exist
mkdir -p logs
echo "✅ Logs directory created"

# Check if holdings.json exists
if [ ! -f "src/main/resources/holdings.json" ]; then
    echo "❌ holdings.json not found. Please create your holdings configuration."
    exit 1
fi

echo "✅ Holdings configuration found"

# Build the application
echo "🔨 Building application..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check for compilation errors."
    exit 1
fi

echo "✅ Build successful"

# Start the application
echo "🎯 Starting Crypto Advisory Notifier..."
echo "📊 The service will run daily at 7:30 AM Asia/Ho_Chi_Minh"
echo "🌐 Manual trigger available at: http://localhost:8080/api/trigger-advisory"
echo "❤️  Health check available at: http://localhost:8080/api/health"
echo ""
echo "Press Ctrl+C to stop the service"
echo "----------------------------------------"

mvn spring-boot:run
