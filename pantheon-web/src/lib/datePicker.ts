import type { Directive } from 'vue'

/**
 * Opens the native date picker on any click inside the field, not just its small calendar
 * icon — browsers otherwise only open it when the icon itself is clicked.
 */
export const vDatePicker: Directive<HTMLInputElement> = {
  mounted(el) {
    el.addEventListener('click', () => {
      try {
        el.showPicker?.()
      } catch {
        // Unsupported or not user-activated — the field still works for typing.
      }
    })
  },
}
