import { route } from 'quasar/wrappers'
import { createRouter, createWebHistory } from 'vue-router'
import routes from './routes'
import { useAuthStore } from 'src/stores/auth'

export default route(() => {
  const router = createRouter({
    scrollBehavior: () => ({ left: 0, top: 0 }),
    routes,
    history: createWebHistory()
  })

  router.beforeEach(async (to) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return { name: 'signin' }
    }

    if ((to.name === 'signin' || to.name === 'signup') && authStore.isAuthenticated) {
      return { name: 'home' }
    }
  })

  return router
})
