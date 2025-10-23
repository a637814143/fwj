<template>
  <div ref="root" class="interface-settings">
    <button
      type="button"
      class="settings-toggle"
      :aria-expanded="open"
      @click="togglePanel"
    >
      {{ t('settings.trigger') }}
    </button>
    <transition name="settings-fade">
      <div
        v-if="open"
        class="settings-panel surface-card"
        role="dialog"
        :aria-label="t('settings.title')"
        @click.stop
      >
        <header class="panel-header">
          <h3>{{ t('settings.title') }}</h3>
          <button type="button" class="close-button" @click="closePanel">
            {{ t('settings.close') }}
          </button>
        </header>
        <div class="panel-section">
          <span class="section-title">{{ t('settings.theme.title') }}</span>
          <div class="options">
            <label v-for="option in themeOptions" :key="option.value" class="option">
              <input type="radio" name="ui-theme" :value="option.value" v-model="theme" />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>
        <div class="panel-section">
          <span class="section-title">{{ t('settings.language.title') }}</span>
          <div class="options">
            <label v-for="option in languageOptions" :key="option.value" class="option">
              <input type="radio" name="ui-language" :value="option.value" v-model="language" />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>
        <div class="panel-section">
          <span class="section-title">{{ t('settings.fontSize.title') }}</span>
          <div class="options">
            <label v-for="option in fontSizeOptions" :key="option.value" class="option">
              <input type="radio" name="ui-font" :value="option.value" v-model="fontSize" />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, inject, onBeforeUnmount, onMounted, ref } from 'vue';

const injectedSettings = inject('appSettings', null);
const translate = inject('translate', (key) => key);

const open = ref(false);
const root = ref(null);

const theme = computed({
  get: () => injectedSettings?.theme ?? 'light',
  set: (value) => {
    if (injectedSettings) {
      injectedSettings.theme = value;
    }
  }
});

const language = computed({
  get: () => injectedSettings?.language ?? 'zh',
  set: (value) => {
    if (injectedSettings) {
      injectedSettings.language = value;
    }
  }
});

const fontSize = computed({
  get: () => injectedSettings?.fontSize ?? 'medium',
  set: (value) => {
    if (injectedSettings) {
      injectedSettings.fontSize = value;
    }
  }
});

const themeOptions = computed(() => [
  { value: 'light', label: translate('settings.theme.light') },
  { value: 'dark', label: translate('settings.theme.dark') }
]);

const languageOptions = computed(() => [
  { value: 'zh', label: translate('settings.language.zh') },
  { value: 'en', label: translate('settings.language.en') }
]);

const fontSizeOptions = computed(() => [
  { value: 'small', label: translate('settings.fontSize.small') },
  { value: 'medium', label: translate('settings.fontSize.medium') },
  { value: 'large', label: translate('settings.fontSize.large') }
]);

const togglePanel = () => {
  open.value = !open.value;
};

const closePanel = () => {
  open.value = false;
};

const handleKeydown = (event) => {
  if (event.key === 'Escape') {
    open.value = false;
  }
};

const handleClickOutside = (event) => {
  if (!root.value) {
    return;
  }
  if (!root.value.contains(event.target)) {
    open.value = false;
  }
};

onMounted(() => {
  if (typeof window !== 'undefined') {
    window.addEventListener('keydown', handleKeydown);
    window.addEventListener('click', handleClickOutside);
  }
});

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('keydown', handleKeydown);
    window.removeEventListener('click', handleClickOutside);
  }
});
</script>

<style scoped>
.interface-settings {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.settings-toggle {
  border: 1px solid var(--color-border);
  background: rgba(255, 255, 255, 0.65);
  color: var(--color-text-strong);
  border-radius: var(--radius-pill);
  padding: 0.45rem 1.2rem;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  gap: 0.45rem;
  box-shadow: var(--shadow-sm);
  backdrop-filter: blur(calc(var(--glass-blur) / 3));
}

.settings-toggle:hover {
  background: rgba(37, 99, 235, 0.12);
  color: var(--color-text-strong);
}

.settings-panel {
  position: absolute;
  top: calc(100% + 0.75rem);
  right: 0;
  width: min(320px, 80vw);
  padding: 1.2rem;
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
  background: var(--gradient-surface);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: 1.1rem;
  z-index: 20;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.panel-header h3 {
  margin: 0;
  font-size: 1.05rem;
}

.close-button {
  border: none;
  background: rgba(148, 163, 184, 0.16);
  color: var(--color-text-soft);
  border-radius: var(--radius-pill);
  padding: 0.35rem 0.9rem;
  font-size: 0.85rem;
}

.close-button:hover {
  background: rgba(148, 163, 184, 0.28);
  color: var(--color-text-strong);
}

.panel-section {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

.section-title {
  font-weight: 600;
  color: var(--color-text-strong);
}

.options {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.option {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.8rem;
  border-radius: var(--radius-pill);
  background: rgba(255, 255, 255, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.25);
  cursor: pointer;
  transition: background var(--transition-base), border-color var(--transition-base),
    color var(--transition-base);
}

.option input[type='radio'] {
  accent-color: #2563eb;
}

.option:hover {
  background: rgba(37, 99, 235, 0.14);
  border-color: rgba(37, 99, 235, 0.4);
}

.settings-fade-enter-active,
.settings-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.settings-fade-enter-from,
.settings-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}
</style>
