#!/bin/bash




set -e


RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo ""
echo "${BLUE}🎰 TruHoldem Release Script${NC}"
echo "=============================="


CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo "${RED}Error: Releases must be made from 'main' branch${NC}"
    echo "Current branch: $CURRENT_BRANCH"
    exit 1
fi


if [ -n "$(git status --porcelain)" ]; then
    echo "${RED}Error: Working directory not clean${NC}"
    echo "Commit or stash your changes first"
    exit 1
fi


echo "${YELLOW}→ Pulling latest changes...${NC}"
git pull origin main


CURRENT_VERSION=$(node -p "require('./package.json').version")
echo "Current version: v$CURRENT_VERSION"


BUMP_TYPE=${1:-patch}

case $BUMP_TYPE in
    patch|minor|major)
        echo "Bump type: $BUMP_TYPE"
        ;;
    *)
        echo "${RED}Error: Invalid bump type '$BUMP_TYPE'${NC}"
        echo "Usage: $0 [patch|minor|major]"
        exit 1
        ;;
esac


IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

case $BUMP_TYPE in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
echo ""
echo "${GREEN}New version: v$NEW_VERSION${NC}"


echo ""
read -p "Continue with release v$NEW_VERSION? (y/N) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Release cancelled"
    exit 0
fi

echo ""
echo "${YELLOW}→ Updating version numbers...${NC}"


node -e "
const fs = require('fs');
const pkg = JSON.parse(fs.readFileSync('package.json', 'utf8'));
pkg.version = '$NEW_VERSION';
fs.writeFileSync('package.json', JSON.stringify(pkg, null, 2) + '\n');
"
echo "  ✓ Updated package.json"


node -e "
const fs = require('fs');
const pkg = JSON.parse(fs.readFileSync('frontend/package.json', 'utf8'));
pkg.version = '$NEW_VERSION';
fs.writeFileSync('frontend/package.json', JSON.stringify(pkg, null, 2) + '\n');
"
echo "  ✓ Updated frontend/package.json"


sed -i.bak "s/<version>.*<\/version><!-- PROJECT_VERSION -->/<version>$NEW_VERSION<\/version><!-- PROJECT_VERSION -->/" backend/pom.xml 2>/dev/null || \
sed -i '' "s/<version>.*<\/version><!-- PROJECT_VERSION -->/<version>$NEW_VERSION<\/version><!-- PROJECT_VERSION -->/" backend/pom.xml 2>/dev/null || \
echo "  ⚠ Could not update pom.xml (add <!-- PROJECT_VERSION --> comment after version)"
rm -f backend/pom.xml.bak
echo "  ✓ Updated backend/pom.xml (if marked)"


echo ""
echo "${YELLOW}→ Running tests...${NC}"

echo "  → Backend tests..."
cd backend && ./mvnw test -q && cd ..
echo "  ${GREEN}✓ Backend tests passed${NC}"

echo "  → Frontend tests..."
cd frontend && npm run test:ci --silent && cd ..
echo "  ${GREEN}✓ Frontend tests passed${NC}"

echo "  → Lint..."
cd frontend && npm run lint --silent && cd ..
echo "  ${GREEN}✓ Lint passed${NC}"


echo ""
echo "${YELLOW}→ Creating release commit...${NC}"
git add package.json frontend/package.json backend/pom.xml
git commit -m "chore: release v$NEW_VERSION"


echo ""
echo "${YELLOW}→ Creating and pushing tag...${NC}"
git tag -a "v$NEW_VERSION" -m "Release v$NEW_VERSION"
git push origin main
git push origin "v$NEW_VERSION"


echo ""
echo "=============================="
echo "${GREEN}✓ Release v$NEW_VERSION complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. GitHub Actions will build and push Docker images"
echo "  2. A GitHub Release will be created automatically"
echo "  3. Deploy to production when ready"
echo ""
echo "Monitor CI/CD: https://github.com/aporkolab/TruHoldem/actions"
