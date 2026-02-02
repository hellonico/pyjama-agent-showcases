# CSS in Showcases

## Structure

The framework provides a base CSS file that should be copied to each showcase:

```
showcase-framework/
  public/
    showcase.css          # Base framework styles (source of truth)

image-generator-agent/
  public/
    showcase.css          # Copied from framework
    style.css             # Image-specific styles

movie-review-agent/
  public/
    css/
      showcase.css        # Copied from framework
      styles.css          # Movie-specific styles
```

## Setup

1. **Copy framework CSS** to your showcase:
   ```bash
   cp ../showcase-framework/public/showcase.css public/
   ```

2. **Include in HTML** (before your custom CSS):
   ```html
   <link rel="stylesheet" href="showcase.css">
   <link rel="stylesheet" href="style.css">
   ```

3. **Create custom CSS** for showcase-specific styles only

## Why Copy Instead of Link?

Browser CSS links must be served by the web server. The path `../showcase-framework/public/showcase.css` is a file system path that won't work when the page is served at `http://localhost:8020`.

Options considered:
- ❌ Relative file path - Browser can't access file system
- ❌ Symlinks - Complicated, platform-specific
- ✅ **Copy the file** - Simple, works everywhere

## Updating Framework CSS

When the framework CSS changes:

```bash
# From showcase directory
cp ../showcase-framework/public/showcase.css public/
```

Or create a script in your showcase:

```bash
#!/bin/bash
# update-framework-css.sh
cp ../showcase-framework/public/showcase.css public/
echo "✅ Framework CSS updated"
```

## CSS Layering

The framework provides:
- Base colors and typography
- Common component styles (.input-field, .action-button, etc.)
- Layout containers (.container, .section, etc.)
- Progress bars, loading spinners, error displays

Your showcase CSS should only contain:
- Showcase-specific components
- Custom layouts
- Domain-specific styling

### Example (Image Generator)

**showcase.css** (from framework):
```css
.input-field { /* base input styles */ }
.action-button { /* base button styles */ }
.progress-bar { /* base progress styles */ }
```

**style.css** (image-specific):
```css
.dimension-controls { /* image dimension UI */ }
.preset-buttons { /* size presets */ }
.image-container { /* image display */ }
.download-btn { /* download button */ }
```

## Available Classes

From `showcase.css`:

### Layout
- `.container`, `.showcase-container` - Main container
- `.input-section` - Input area wrapper
- `.result-section` - Result display wrapper
- `.section` - Generic section

### Inputs
- `.input-field` - Text inputs, textareas
- `.number-input` - Number input container
- `.action-button` - Primary action buttons

### Feedback
- `.progress-section` - Progress wrapper
- `.progress-bar-container` - Progress bar track
- `.progress-bar` - Progress bar fill
- `.loading-section` - Loading spinner area
- `.error-section` - Error display

### Typography
- `h1`, `h2`, `h3` - Pre-styled headers
- `.subtitle` - Subheadings
- `.progress-text` - Progress messages

## Quick Start

For a new showcase:

1. Copy framework CSS:
   ```bash
   cp ../showcase-framework/public/showcase.css public/
   ```

2. Create your HTML:
   ```html
   <link rel="stylesheet" href="showcase.css">
   <link rel="stylesheet" href="custom.css">
   ```

3. Create `custom.css` with only your unique styles:
   ```css
   /* Only showcase-specific styles here! */
   .my-custom-component {
       /* your styles */
   }
   ```
