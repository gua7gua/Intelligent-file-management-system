import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Compilation from './index.vue'

vi.mock('element-plus', () => ({
  ElMessage: { success: () => {}, warning: () => {} },
  ElMessageBox: { confirm: () => Promise.reject(new Error('cancel')) },
}))

async function mountComponent() {
  const router = createRouter({ history: createMemoryHistory(), routes: [{ path: '/admin/compilation', component: Compilation }] })
  await router.push('/admin/compilation')
  await router.isReady()
  const wrapper = mount(Compilation, { global: { plugins: [router] } })
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
  return wrapper
}

describe('Compilation', () => {
  it('renders four status metric cards', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.findAll('.metric').length).toBe(4)
    expect(wrapper.text()).toContain('草稿')
    expect(wrapper.text()).toContain('已入库')
  })

  it('renders compilation list across statuses', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.findAll('tbody tr').length).toBeGreaterThan(0)
    expect(wrapper.text()).toContain('BY-202606-001')
  })

  it('opens editor and clears title on new button click', async () => {
    const wrapper = await mountComponent()
    expect(wrapper.text()).toContain('成果编辑')
    expect((wrapper.find('#title').element as HTMLInputElement).value).not.toBe('')
    await wrapper.find('button.new-compilation').trigger('click')
    await new Promise((r) => setTimeout(r, 10))
    await flushPromises()
    expect((wrapper.find('#title').element as HTMLInputElement).value).toBe('')
  })

  it('loads a compilation into editor on row edit button click', async () => {
    const wrapper = await mountComponent()
    const editButtons = wrapper.findAll('tbody tr').map((row) => row.find('button'))
    await editButtons[1].trigger('click')
    await new Promise((r) => setTimeout(r, 30))
    await flushPromises()
    expect((wrapper.find('#title').element as HTMLInputElement).value).toContain('工业园')
  })
})
