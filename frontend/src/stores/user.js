import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    roleCode: s => s.user?.roleCode || '',
    isAdmin: s => s.user?.roleCode === 'ADMIN',
    isTrainer: s => s.user?.roleCode === 'TRAINER',
    isEmployee: s => s.user?.roleCode === 'EMPLOYEE',
    canManage: s => s.user?.roleCode === 'ADMIN' || s.user?.roleCode === 'TRAINER'
  },
  actions: {
    async login(username, password) {
      const data = await request.post('/auth/login', { username, password })
      this.token = data.token
      this.user = data.user
      localStorage.setItem('token', data.token)
      localStorage.setItem('user', JSON.stringify(data.user))
      return data.user
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
