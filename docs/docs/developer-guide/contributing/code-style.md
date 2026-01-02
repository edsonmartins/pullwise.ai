# Code Style

Coding standards for Pullwise project.

## Overview

Consistent code style improves readability and maintainability. This guide covers conventions for both backend (Java) and frontend (TypeScript).

## Java Code Style

### Naming Conventions

```java
// Classes: PascalCase
public class ReviewService { }

// Interfaces: PascalCase, often with 'I' prefix
public interface IPlugin { }

// Methods: camelCase
public void executeReview() { }

// Variables: camelCase
private String reviewId;

// Constants: UPPER_SNAKE_CASE
private static final int MAX_RETRIES = 3;

// Packages: lowercase
package com.pullwise.api.application.service;
```

### Class Structure

```java
// 1. Package declaration
package com.pullwise.api.domain.model;

// 2. Imports (grouped and sorted)
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

// 3. Javadoc
/**
 * Review aggregate root.
 *
 * <p>Manages the code review lifecycle including
 * SAST and LLM analysis passes.
 */
// 4. Class annotation
@Entity
@Table(name = "reviews")
// 5. Class declaration
public class Review {

    // 6. Static fields
    private static final Logger log = LoggerFactory.getLogger(Review.class);

    // 7. Instance fields (public -> protected -> package -> private)
    private Long id;
    private String status;

    // 8. Constructors
    public Review() { }

    public Review(String status) {
        this.status = status;
    }

    // 9. Methods (public -> protected -> package -> private)
    public void execute() { }
}
```

### Method Ordering

```java
public class ReviewService {

    // 1. Public API methods
    public Review createReview(CreateReviewRequest request) { }

    public Review getReview(Long id) { }

    // 2. Package protected methods
    void processReview(Review review) { }

    // 3. Private helper methods
    private List<Issue> consolidateIssues(List<Issue> sast, List<Issue> llm) { }

    // 4. Getters and setters (if needed)
    public Long getId() { return id; }
}
```

### Indentation and Spacing

```java
// Use 4 spaces for indentation (NO tabs)
public class Example {
    private String field;

    public void method(String param1, String param2,
                      String param3) {
        // Blank line between sections
        if (condition) {
            doSomething();
        }

        // Spaces around operators
        int result = a + b * c;

        // Spaces after commas
        method(arg1, arg2, arg3);

        // No trailing spaces
    }
}
```

### Annotations

```java
// One annotation per line (when multiple)
@Entity
@Table(name = "reviews")
public class Review {
    // Single annotation on same line
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Multiple annotations
    @OneToMany(mappedBy = "review")
    @OrderBy("createdAt DESC")
    private List<Issue> issues;
}
```

## TypeScript Code Style

### Naming Conventions

```typescript
// Classes/Interfaces/Types: PascalCase
class ReviewService { }
interface PluginConfig { }
type ReviewStatus = 'pending' | 'completed';

// Functions/Variables: camelCase
const reviewCount = 10;
function getReview() { }

// Constants: UPPER_SNAKE_CASE
const MAX_RETRIES = 3;

// Private properties: prefix with underscore
private _httpClient: HttpClient;

// React components: PascalCase
function ReviewList() { }

// Custom hooks: camelCase with 'use' prefix
function useReviews() { }
```

### Component Structure

```typescript
// 1. Imports
import { useState, useEffect } from 'react';
import { Review } from '@/types';

// 2. Type definitions
interface ReviewListProps {
  projectId: number;
  onReviewClick: (review: Review) => void;
}

// 3. Component declaration
export function ReviewList({ projectId, onReviewClick }: ReviewListProps) {
  // 4. Hooks (state, effects, refs)
  const [reviews, setReviews] = useState<Review[]>([]);

  useEffect(() => {
    loadReviews();
  }, [projectId]);

  // 5. Event handlers
  const handleClick = (review: Review) => {
    onReviewClick(review);
  };

  // 6. Derived values
  const completedCount = reviews.filter(r => r.status === 'completed').length;

  // 7. Render
  return (
    <div>
      {reviews.map(review => (
        <ReviewCard key={review.id} review={review} onClick={handleClick} />
      ))}
    </div>
  );
}
```

### Hooks Order

```typescript
function MyComponent() {
  // 1. State hooks
  const [state, setState] = useState();
  const [ref, setRef] = useState();

  // 2. Ref hooks
  const inputRef = useRef<HTMLInputElement>();

  // 3. Effect hooks
  useEffect(() => {}, []);

  // 4. Custom hooks
  const { data, loading } = useData();

  // 5. Functions
  const handleClick = () => {};

  // 6. Render
  return <div />;
}
```

### Indentation and Spacing

```typescript
// Use 2 spaces for indentation
function example(
  param1: string,
  param2: number,
  param3: boolean
): string {
  // Spaces around operators
  const result = a + b * c;

  // Spaces after commas
  array.map(item => item.value);

  // No trailing commas in function params (TypeScript)
  // Trailing commas in arrays/objects (multi-line)
  const obj = {
    a: 1,
    b: 2,
    c: 3,
  };

  return result;
}
```

## General Conventions

### Line Length

- **Maximum:** 120 characters
- **Preferred:** 80-100 characters

```java
// Break long lines
String message = String.format(
    "Review %s has %d issues with %s severity",
    reviewId,
    count,
    severity
);
```

### Imports

```java
// Java: Group and sort imports
// 1. Java standard library
import java.util.List;
import java.util.ArrayList;

// 2. Third-party libraries
import org.springframework.stereotype.Service;

// 3. Project imports
import com.pullwise.api.domain.model.Review;
```

```typescript
// TypeScript: Group and sort imports
// 1. External libraries
import { useState } from 'react';
import { Button } from '@mui/material';

// 2. Internal imports (absolute)
import { Review } from '@/types';
import { useReviews } from '@/hooks';

// 3. Relative imports
import { ReviewCard } from './ReviewCard';
```

### Comments

```java
/**
 * Javadoc for public APIs.
 *
 * <p>Additional details here.
 *
 * @param request The review request
 * @return The created review
 * @throws PluginException if plugin fails
 */
public Review createReview(CreateReviewRequest request) throws PluginException {
    // Inline comments for "why", not "what"
    // Use retry for transient failures
    return retryTemplate.execute(() -> plugin.analyze(request));
}
```

```typescript
/**
 * Creates a new review.
 *
 * @param projectId - The project ID
 * @returns The created review
 */
export async function createReview(projectId: number): Promise<Review> {
  // Use timeout for API calls to prevent hanging
  return await timeout(fetch(`/api/projects/${projectId}/reviews`), 5000);
}
```

## Error Handling

### Java

```java
// Specific exceptions
try {
    reviewService.execute(request);
} catch (PluginException e) {
    log.error("Plugin execution failed: {}", e.getMessage());
    throw new ReviewException("Review failed", e);
} catch (Exception e) {
    log.error("Unexpected error", e);
    throw new ReviewException("Unexpected error", e);
}

// Never catch Throwable
try {
    // Bad: catches Error too
} catch (Throwable t) { }

// Good: specific exceptions
try {
} catch (IOException | PluginException e) { }
```

### TypeScript

```typescript
// Always handle errors
try {
  await createReview(projectId);
} catch (error) {
  if (error instanceof ApiError) {
    console.error('API error:', error.message);
  } else {
    console.error('Unknown error:', error);
  }
}

// Use error boundaries
<ErrorBoundary fallback={<ErrorPage />}>
  <ReviewList />
</ErrorBoundary>
```

## Logging

### Java (SLF4J)

```java
// Always use parameterized logging
log.info("Review {} started at {}", reviewId, timestamp);

// NOT this (string concatenation)
log.info("Review " + reviewId + " started at " + timestamp);

// Log levels
log.error("Critical error: {}", error);  // Errors
log.warn("Retrying operation, attempt {}", attempt);  // Warning
log.info("Review completed successfully");  // Information
log.debug("Plugin configuration: {}", config);  // Debug
```

### TypeScript (console)

```typescript
// Use appropriate log level
console.error('Critical error:', error);
console.warn('Warning: retrying...');
console.info('Operation completed');
console.debug('Debug info:', data);
```

## Null Safety

### Java

```java
// Use @NonNull annotations
public void processReview(@NonNull ReviewRequest request) {
    // IDE will warn if null passed
}

// Use Optional for return values
public Optional<Review> findById(Long id) {
    return reviewRepository.findById(id);
}

// Use Optional for nullable parameters
public void updateConfig(Optional<Config> config) {
    config.ifPresentOrElse(
        this::applyConfig,
        () -> useDefaultConfig()
    );
}
```

### TypeScript

```typescript
// Enable strict mode
// tsconfig.json
{
  "strict": true,
  "strictNullChecks": true
}

// Use type guards
function isReview(obj: unknown): obj is Review {
  return typeof obj === 'object' && obj !== null && 'id' in obj;
}

// Optional chaining
const owner = review?.project?.owner?.name;

// Nullish coalescing
const name = review?.name ?? 'Unknown';
```

## Testing Style

### Test Naming

```java
// Given-When-Then pattern
@Test
void givenInvalidRequest_whenCreateReview_thenThrowException() {
    // Given
    CreateReviewRequest request = invalidRequest();

    // When & Then
    assertThatThrownBy(() -> reviewService.create(request))
        .isInstanceOf(ValidationException.class);
}
```

```typescript
// Arrange-Act-Assert pattern
describe('createReview', () => {
  it('should throw error when request is invalid', async () => {
    // Arrange
    const request = invalidRequest();

    // Act & Assert
    await expect(createReview(request)).rejects.toThrow(ValidationException);
  });
});
```

## Linting Configuration

### Java (Checkstyle)

```xml
<!-- checkstyle.xml -->
<module name="Checker">
    <property name="severity" value="warning"/>
    <module name="TreeWalker">
        <module name="Indentation">
            <property name="basicOffset" value="4"/>
        </module>
        <module name="LineLength">
            <property name="max" value="120"/>
        </module>
        <module name="MemberName">
            <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
        </module>
    </module>
</module>
```

### TypeScript (ESLint + Prettier)

```json
// .eslintrc.json
{
  "extends": [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "plugin:react/recommended",
    "prettier"
  ],
  "rules": {
    "@typescript-eslint/no-unused-vars": "error",
    "react/react-in-jsx-scope": "off"
  }
}
```

```json
// .prettierrc
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100
}
```

## Pre-commit Hooks

```bash
#!/bin/bash
# .git/hooks/pre-commit

# Backend format
cd backend
./mvnw spotless:check

# Frontend lint
cd ../frontend
npm run lint
npm run type-check

# Run tests
npm test -- --passWithNoTests
```

## Best Practices

### 1. Keep Methods Short

```java
// Good: Focused, single responsibility
public Review createReview(CreateReviewRequest request) {
    validateRequest(request);
    Review review = initializeReview(request);
    processAnalysis(review);
    return reviewRepository.save(review);
}

// Bad: Too long, doing too much
public Review createReview(CreateReviewRequest request) {
    // 100 lines of logic...
}
```

### 2. Extract Magic Numbers

```java
// Bad
if (retryCount > 3) { }

// Good
private static final int MAX_RETRIES = 3;
if (retryCount > MAX_RETRIES) { }
```

### 3. Use Meaningful Names

```java
// Bad
var d = getData();

// Good
var reviewData = fetchReviewData();
```

### 4. Early Returns

```java
// Good: Early returns reduce nesting
public void process(Review review) {
    if (review == null) {
        return;
    }
    if (!review.isValid()) {
        return;
    }
    // Main logic
}

// Bad: Deep nesting
public void process(Review review) {
    if (review != null) {
        if (review.isValid()) {
            // Main logic
        }
    }
}
```

## Next Steps

- [Testing](/docs/developer-guide/contributing/testing) - Testing guidelines
- [Pull Requests](/docs/developer-guide/contributing/pull-requests) - PR guide
- [Documentation](/docs/developer-guide/contributing/documentation) - Documentation guide
