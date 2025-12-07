#!/bin/bash

# ============================================================================
# Spring Boot Project Initializer
#
# This script initializes a new Spring Boot project from the template.
# It replaces all template placeholders with your project-specific values.
#
# Usage: ./init-project.sh <project-name> <package-name>
# Example: ./init-project.sh my-awesome-app com.company.myapp
# ============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print functions
print_info() {
    echo -e "${BLUE}ℹ${NC} $1"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# Validate arguments
if [ $# -lt 2 ]; then
    echo ""
    echo "Usage: $0 <project-name> <package-name>"
    echo ""
    echo "Arguments:"
    echo "  project-name  - Name of your project (e.g., my-awesome-app)"
    echo "  package-name  - Java package name (e.g., com.company.myapp)"
    echo ""
    echo "Example:"
    echo "  $0 my-awesome-app com.company.myapp"
    echo ""
    exit 1
fi

PROJECT_NAME="$1"
PACKAGE_NAME="$2"

# Validate project name (alphanumeric and hyphens only)
if ! [[ "$PROJECT_NAME" =~ ^[a-z][a-z0-9-]*$ ]]; then
    print_error "Invalid project name: $PROJECT_NAME"
    echo "Project name must start with a lowercase letter and contain only lowercase letters, numbers, and hyphens."
    exit 1
fi

# Validate package name
if ! [[ "$PACKAGE_NAME" =~ ^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$ ]]; then
    print_error "Invalid package name: $PACKAGE_NAME"
    echo "Package name must follow Java package naming conventions (e.g., com.company.myapp)."
    exit 1
fi

# Template values
OLD_PACKAGE="com.template.app"
OLD_PROJECT="template-app"
OLD_APP_CLASS="TemplateApplication"

# New values
PACKAGE_PATH=$(echo "$PACKAGE_NAME" | tr '.' '/')
APP_CLASS_NAME=$(echo "$PROJECT_NAME" | sed -E 's/(^|-)([a-z])/\U\2/g')Application

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║          Spring Boot Project Initializer                       ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
print_info "Project Name: $PROJECT_NAME"
print_info "Package Name: $PACKAGE_NAME"
print_info "Package Path: $PACKAGE_PATH"
print_info "Main Class:   $APP_CLASS_NAME"
echo ""

# Confirm before proceeding
read -p "Proceed with initialization? [y/N] " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warning "Initialization cancelled."
    exit 1
fi

echo ""

# Step 1: Create new directory structure
print_info "Creating new package directory structure..."
NEW_JAVA_DIR="src/main/java/$PACKAGE_PATH"
NEW_TEST_DIR="src/test/java/$PACKAGE_PATH"

mkdir -p "$NEW_JAVA_DIR"
mkdir -p "$NEW_TEST_DIR"
print_success "Created directories"

# Step 2: Move Java files to new package
print_info "Moving Java source files..."
OLD_JAVA_DIR="src/main/java/com/template/app"

if [ -d "$OLD_JAVA_DIR" ]; then
    # Move all subdirectories
    for dir in "$OLD_JAVA_DIR"/*; do
        if [ -d "$dir" ]; then
            mv "$dir" "$NEW_JAVA_DIR/"
        fi
    done

    # Move any remaining files (like the main Application class)
    for file in "$OLD_JAVA_DIR"/*.java; do
        if [ -f "$file" ]; then
            mv "$file" "$NEW_JAVA_DIR/"
        fi
    done

    # Remove old directory structure
    rm -rf "src/main/java/com/template"
fi
print_success "Moved source files"

# Step 3: Move test files
print_info "Moving test files..."
OLD_TEST_DIR="src/test/java/com/template/app"

if [ -d "$OLD_TEST_DIR" ]; then
    for dir in "$OLD_TEST_DIR"/*; do
        if [ -d "$dir" ]; then
            mv "$dir" "$NEW_TEST_DIR/"
        fi
    done

    for file in "$OLD_TEST_DIR"/*.java; do
        if [ -f "$file" ]; then
            mv "$file" "$NEW_TEST_DIR/"
        fi
    done

    rm -rf "src/test/java/com/template"
fi
print_success "Moved test files"

# Step 4: Detect OS for sed compatibility
if [[ "$OSTYPE" == "darwin"* ]]; then
    SED_INPLACE="sed -i ''"
else
    SED_INPLACE="sed -i"
fi

# Step 5: Replace package names in Java files
print_info "Updating package declarations..."
find . -type f -name "*.java" -exec $SED_INPLACE "s/$OLD_PACKAGE/$PACKAGE_NAME/g" {} \;
print_success "Updated package declarations"

# Step 6: Update application class name
print_info "Renaming main application class..."
OLD_APP_FILE="$NEW_JAVA_DIR/TemplateApplication.java"
NEW_APP_FILE="$NEW_JAVA_DIR/${APP_CLASS_NAME}.java"

if [ -f "$OLD_APP_FILE" ]; then
    mv "$OLD_APP_FILE" "$NEW_APP_FILE"
    $SED_INPLACE "s/$OLD_APP_CLASS/$APP_CLASS_NAME/g" "$NEW_APP_FILE"
fi
print_success "Renamed application class"

# Step 7: Update configuration files
print_info "Updating configuration files..."

# Update settings.gradle
$SED_INPLACE "s/$OLD_PROJECT/$PROJECT_NAME/g" settings.gradle

# Update application.yml files
find . -type f -name "application*.yml" -exec $SED_INPLACE "s/$OLD_PROJECT/$PROJECT_NAME/g" {} \;

# Update docker-compose files
find . -type f -name "docker-compose*.yml" -exec $SED_INPLACE "s/template-/$PROJECT_NAME-/g" {} \;
find . -type f -name "docker-compose*.yml" -exec $SED_INPLACE "s/template_/$PROJECT_NAME/g" {} \;

# Update Dockerfile
if [ -f "Dockerfile" ]; then
    $SED_INPLACE "s/template/$PROJECT_NAME/g" Dockerfile
fi

print_success "Updated configuration files"

# Step 8: Update Flyway migration comments
print_info "Updating Flyway migrations..."
find . -type f -name "*.sql" -exec $SED_INPLACE "s/template_/$PROJECT_NAME/g" {} \;
print_success "Updated migrations"

# Step 9: Clean up backup files (if any)
print_info "Cleaning up..."
find . -name "*.bak" -delete 2>/dev/null || true
find . -name "*-e" -delete 2>/dev/null || true
find . -name "*.orig" -delete 2>/dev/null || true
print_success "Cleanup complete"

# Step 10: Remove this script and template docs
print_info "Removing template files..."
rm -f init-project.sh
rm -rf docs/plans 2>/dev/null || true
print_success "Removed template files"

echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                    Initialization Complete!                     ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
print_success "Project '$PROJECT_NAME' has been initialized successfully!"
echo ""
echo "Next steps:"
echo "  1. Review and update .env file with your configuration"
echo "  2. Start development environment:"
echo "     $ docker-compose -f docker-compose.dev.yml up -d"
echo "  3. Run the application:"
echo "     $ ./gradlew bootRun"
echo ""
echo "Happy coding! 🚀"
echo ""
