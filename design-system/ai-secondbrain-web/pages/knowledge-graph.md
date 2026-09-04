# Knowledge Graph Page Overrides

> **PROJECT:** AI SecondBrain Web  
> **Page type:** Interactive knowledge-graph workspace  
> **Updated:** 2026-09-03

This page intentionally differs from the normal light application shell: the graph canvas remains dark in either application theme so relationships retain contrast and spatial focus.

## Layout

- Desktop uses a compact header so the graph canvas consumes the remaining viewport height rather than ending in page-level whitespace.
- Header: page name, concise exploration cue, and live node/relationship counts.
- Toolbar: local search, mastery filter, fit, reset, and inspector visibility. Controls keep a 42–44px target and visible keyboard focus.
- Main area: one large force-graph canvas. At desktop widths the node inspector is a fixed right rail; below 960px it becomes a modal-style side drawer so the canvas retains usable width.
- The light/dark global shell is unchanged; only the canvas and inspector use the dark workspace surface.

## Visual language

- Canvas: near-black forest surface (`#151c19`) with a low-contrast 28px grid and one soft sage focal glow. The texture supports orientation but never competes with graph labels.
- Nodes: size means importance; mastery is communicated with a muted semantic palette (sage → brass → coral → slate), never color alone.
- Edges: quiet, thin default connections; a selected node elevates only its one-hop neighborhood.
- Labels: show only important nodes by default. Selected and search-matched nodes expand to their full title; hover uses a high-contrast card with the complete title, mastery and importance so every node remains identifiable.

## Interaction and accessibility

- ECharts supports wheel zoom, canvas pan, and temporary node dragging. Positions are session-only.
- Clicking a node selects its one-hop neighborhood; clicking blank canvas or pressing Escape clears the focus.
- Search selects the first local match without making a network call. The mastery filter only uses the loaded data.
- The chart has an ARIA summary, the toolbar is keyboard reachable, and state changes are announced in a polite live region.
- Loading, empty, filtered-empty, and error states always use the real API outcome; never substitute sample nodes.

## Boundaries

- This is an exploration workspace, not a relationship editor.
- No layout persistence, AI relation generation, or graph mutations belong to this page.
- Knowledge details open through the existing `/knowledge/:id` route.
