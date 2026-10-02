---
name: Cipher Bento
colors:
  surface: '#f9f9fd'
  surface-dim: '#d9dade'
  surface-bright: '#f9f9fd'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f3f3f7'
  surface-container: '#ededf2'
  surface-container-high: '#e8e8ec'
  surface-container-highest: '#e2e2e6'
  on-surface: '#1a1c1f'
  on-surface-variant: '#3d4a3f'
  inverse-surface: '#2f3034'
  inverse-on-surface: '#f0f0f4'
  outline: '#6d7b6e'
  outline-variant: '#bccabc'
  surface-tint: '#006d3c'
  primary: '#006d3c'
  on-primary: '#ffffff'
  primary-container: '#12b76a'
  on-primary-container: '#004021'
  inverse-primary: '#51df8e'
  secondary: '#b02f00'
  on-secondary: '#ffffff'
  secondary-container: '#ff5722'
  on-secondary-container: '#541100'
  tertiary: '#5e5c6e'
  on-tertiary: '#ffffff'
  tertiary-container: '#a19eb1'
  on-tertiary-container: '#363545'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#70fda7'
  primary-fixed-dim: '#51df8e'
  on-primary-fixed: '#00210e'
  on-primary-fixed-variant: '#00522c'
  secondary-fixed: '#ffdbd1'
  secondary-fixed-dim: '#ffb5a0'
  on-secondary-fixed: '#3b0900'
  on-secondary-fixed-variant: '#862200'
  tertiary-fixed: '#e4e0f5'
  tertiary-fixed-dim: '#c7c4d8'
  on-tertiary-fixed: '#1b1a29'
  on-tertiary-fixed-variant: '#464555'
  background: '#f9f9fd'
  on-background: '#1a1c1f'
  surface-variant: '#e2e2e6'
typography:
  display-hero:
    fontFamily: Plus Jakarta Sans
    fontSize: 56px
    fontWeight: '800'
    lineHeight: 64px
    letterSpacing: -0.03em
  display-hero-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 38px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
    letterSpacing: -0.005em
  body-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  body-sm:
    fontFamily: Plus Jakarta Sans
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
    letterSpacing: 0em
  label-numeric:
    fontFamily: JetBrains Mono
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 24px
    letterSpacing: -0.02em
  label-code:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.04em
  badge-pill:
    fontFamily: Plus Jakarta Sans
    fontSize: 11px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.02em
rounded:
  sm: 0.5rem
  DEFAULT: 1rem
  md: 1.5rem
  lg: 2rem
  xl: 3rem
  full: 9999px
spacing:
  gutter: 1.25rem
  margin: 1.5rem
  space-xs: 0.375rem
  space-sm: 0.75rem
  space-md: 1.25rem
  space-lg: 1.75rem
  space-xl: 2.5rem
---

## Brand & Style

This design system establishes an expressive, tactile bento-widget aesthetic for a modern secure chat application. Balancing high-grade cryptographic privacy with joyful consumer ergonomics, the interface diverges sharply from cold, surveillance-style utilitarian tools. It treats conversational data, contact keys, security toggles, and media as tangible, physical widgets arranged in an adaptive modular grid.

The design movement fuses **Tactile Pop** with **Bento Modernism**:
- **Tactile Pop:** Elevated cards with physical presence, hyper-rounded containers, thick pill triggers, and lively spring physics that give digital encryption physical mass and delight.
- **Bento Modernism:** Information partitioned into high-contrast modular cells, combining quiet utility with bursts of high-impact accent tones (Coral Orange, Mint Green, Lilac, and Soft Yellow).
- **Emotional Intent:** Trustworthy yet approachable, playful without sacrificing precision, and instantly distinct in a sea of monochrome messenger apps.

## Colors

The palette leverages a foundational split between industrial neutral obsidian, clean frosted canvas tones, and high-energy chromatic accents:

- **Canvas & Base Tones:**
  - `Base Canvas`: `#EBEFF2` — A cool, tinted mist gray that establishes high contrast against pure white cards.
  - `Card Surface`: `#FFFFFF` — Pure white for elevated bento tiles and floating message clusters.
  - `Dark Obsidian (Neutral Primary)`: `#131518` — Deep midnight mineral for primary typography, icons, active tab docks, and tactile boundary states.
  - `Subtle Neutral`: `#667085` — Slate neutral for secondary metadata and inactive timestamps.

- **Signature Accents:**
  - `Mint Green Active`: `#12B76A` — Key action states, cryptographic safety indicators, and active connection halos.
  - `Mint Green Soft`: `#D2F7E1` — Tinted pill backdrops, inbound secure whisper bubbles, and positive state panels.
  - `Vibrant Coral Orange`: `#FF5722` — Attention beacons, urgent ephemeral countdowns, unread count badging, and destruction toggles.
  - `Lilac Wash`: `#EDE9FE` — Auxiliary widget backings, media vault groupings, and secondary user identity pills.
  - `Soft Yellow`: `#FEF08A` — Pinned note highlights, key fingerprint cards, and warning nudges.

## Typography

Typography delivers a high-contrast union between friendly modern grotesque letterforms and rigid technical precision:

- **Primary Typeface (`Plus Jakarta Sans`):** Selected for its geometric stance, wide counters, and rhythmic curves that naturally pair with bulbous bento containers. Display and headline levels rely on heavy weights (Bold, Extra Bold) with tight negative tracking to maintain solid, graphic mass.
- **Technical Monospace (`JetBrains Mono`):** Applied deliberately to cryptographic metrics, key fingerprints, oversized countdown clocks, numeric unread tallies, and hash verify badges.
- **Hierarchy Rules:** Message copy remains balanced and open (`body-lg`, `body-md`) with generous line-height to maintain effortless scannability within dynamic bubble containers. Numeric badges and status indicators always capitalize on bold tabular figures.

## Layout & Spacing

Layouts follow an asymmetrical Bento Grid model that accommodates modular functional cards (conversations, contact dossier, key security monitor, media rolls, ephemeral timers):

- **Grid Architecture:**
  - **Desktop (>= 1024px):** 12-column adaptive fluid canvas within a 1440px max enclosure. Bento tiles span 3, 4, 6, or 8 columns with 20px (`1.25rem`) gutters.
  - **Tablet (768px - 1023px):** 8-column layout with 16px gutters; sidebar docks collapse to an icon rail and cards reflow into 4-column halves.
  - **Mobile (< 768px):** 4-column grid or single-column stacked feed with 16px outer margins. Message threads prioritize vertical full-bleed layouts with floating bottom input docks.

- **Spacing Cadence:**
  - Bento card interior padding standardizes on `space-md` (20px) for compact widgets and `space-lg` (28px) for primary narrative blocks.
  - Interactive element clusters maintain an 8px to 12px separation (`space-xs` to `space-sm`) to support fat-finger accessibility across mobile touch targets.

## Elevation & Depth

This design system avoids traditional fuzzy drop shadows, opting instead for **tactile multi-layered depth** and sharp physical contrast:

- **Level 0 (Canvas Base):** Flat `#EBEFF2`.
- **Level 1 (Bento Tile / Surface):** `#FFFFFF` cards resting on `#EBEFF2` with a subtle contact rim: `0 2px 4px rgba(19, 21, 24, 0.03), 0 12px 24px -4px rgba(19, 21, 24, 0.06)`. Border is crisp: `1px solid rgba(19, 21, 24, 0.05)`.
- **Level 2 (Floating Action Docks & Active Overlays):** Deep layered elevation with subtle obsidian tinting: `0 8px 16px -2px rgba(19, 21, 24, 0.08), 0 24px 48px -8px rgba(19, 21, 24, 0.12)`.
- **Level 3 (Tactile Press & Physics):** Interactive elements translate down by 2px on `:active` with an accelerated cubic bezier curve `cubic-bezier(0.34, 1.56, 0.64, 1)` mimicking physical silicone switchboards. When released, an overshooting spring restores resting position.

## Shapes

The design system establishes a distinctive dual-scale curve aesthetic:

- **Bento Widget Cards:** Large perimeter radiuses ranging from `28px` to `36px` (`rounded-xl` to custom bento scale), giving screens an inviting, sculptural form factor.
- **Pills & Interactive Controls:** Fully rounded `9999px` continuous capsule profiles for buttons, search strips, input fields, notification badges, and chat status tags.
- **Message Bubbles:** Outbound bubbles use continuous capsules with a localized micro-corner (28px on 3 sides, 6px bottom-trailing anchor), creating crisp dialogue directionality while remaining consistent with the card architecture.

## Components

- **Buttons:**
  - *Primary (Obsidian):* Deep `#131518` background, `#FFFFFF` text, fully rounded pill shape (height 48px or 56px). Hover state triggers a gentle scale up (`1.02`), active state yields a tactile press (`translateY(2px)`).
  - *Accent (Coral):* `#FF5722` fill with white text for destructive or critical actions (e.g., "Purge Session", "Burn Keys").
  - *Tonal (Mint / Lilac):* `#D2F7E1` with `#12B76A` text or `#EDE9FE` with `#5B21B6` text. Zero drop shadow, 100% tactile hit target.

- **Bento Cards (Widgets):**
  - Standalone rounded containers (`32px` corner radius) backed by `#FFFFFF` or tinted pastel surfaces (`#D2F7E1`, `#EDE9FE`, `#FEF08A`).
  - Contains modular layouts: an icon badge in the top-left, a status pill in the top-right, followed by bold oversized statistics or micro-conversations.

- **Message Bubbles:**
  - *Inbound:* Pure `#FFFFFF` bubble, `28px` radius with bottom-left anchor at `8px`. Dark Obsidian text with monospace timestamp.
  - *Outbound:* `#131518` background, `#FFFFFF` text, `28px` radius with bottom-right anchor at `8px`. Status tick in Mint Green `#12B76A`.
  - *Encrypted Whisper (Ephemeral):* Bordered container with `#EDE9FE` background, animated dashed border, and Coral `#FF5722` countdown timer badge.

- **Input Fields & Chat Docks:**
  - Floating pill-shaped dock centered at screen base.
  - `#FFFFFF` solid fill with 1.5px subtle border (`rgba(19, 21, 24, 0.08)`) and high elevation. Integrated camera, audio mic, and send actions housed inside dedicated circular icon buttons.

- **Chips & Badges:**
  - Height 24px-28px, continuous capsule (`rounded-full`), `JetBrains Mono` uppercase bold micro-text (`10px`-`11px`).
  - Colors matched to semantic roles: Verification Hash (`#D2F7E1` text `#12B76A`), Unread Counter (`#FF5722` text `#FFFFFF`), Pinned Key (`#FEF08A` text `#854D0E`).

- **Checkboxes, Radios & Switches:**
  - Switches use an exaggerated 32px-height pill track with a floating 26px white ball that squashes slightly along the x-axis (`scaleX(1.15)`) during spring transition.