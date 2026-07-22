import { ref, watch } from 'vue'

const THEME_KEY = 'ai-secondbrain-theme'

const stored = localStorage.getItem(THEME_KEY)
const theme = ref(stored || 'light')

function applyTheme(value) {
  document.documentElement.setAttribute('data-theme', value)
}

export function useThemeStore() {
  function setTheme(value) {
    theme.value = value
    localStorage.setItem(THEME_KEY, value)
    applyTheme(value)
  }

  function toggleTheme() {
    setTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  // Initialize on first access
  if (!document.documentElement.hasAttribute('data-theme')) {
    applyTheme(theme.value)
  }

  return { theme, setTheme, toggleTheme }
}
