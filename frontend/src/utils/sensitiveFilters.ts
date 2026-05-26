// 敏感词过滤工具 - DFA 算法，从后端动态加载词库
import { getSensitiveWords } from '@/api/admin'
import type { SensitiveWord } from '@/api/admin'

// DFA 节点
interface DfaNode {
  isEnd: boolean
  children: Map<string, DfaNode>
}

class SensitiveFilter {
  private root: DfaNode = { isEnd: false, children: new Map() }
  private isLoaded = false
  private loadingPromise: Promise<void> | null = null

  // 从后端加载敏感词库
  async loadWords(): Promise<void> {
    if (this.isLoaded) return
    if (this.loadingPromise) return this.loadingPromise

    this.loadingPromise = (async () => {
      try {
        // 后端返回格式：{ code: 200, data: SensitiveWord[], message: "" }
        const response = await getSensitiveWords()
        // response 是 axios 拦截器返回的完整后端响应对象
        const wordsData = (response as any).data || response
        let words: string[] = []
        if (Array.isArray(wordsData)) {
          words = wordsData.filter((w: SensitiveWord) => w.enabled === 1).map((w: SensitiveWord) => w.word)
        } else if (wordsData?.data && Array.isArray(wordsData.data)) {
          words = wordsData.data.filter((w: SensitiveWord) => w.enabled === 1).map((w: SensitiveWord) => w.word)
        }
        if (words.length === 0) {
          console.warn('敏感词库为空，使用默认词库')
          words = ['色情', '赌博', '毒品', '暴力', '反动', '恐怖'] // 示例默认，可替换
        }
        this.buildDfa(words)
        this.isLoaded = true
      } catch (error) {
        console.error('加载敏感词库失败，使用默认词库', error)
        this.buildDfa(['色情', '赌博', '毒品', '暴力', '反动', '恐怖'])
        this.isLoaded = true
      }
    })()

    return this.loadingPromise
  }

  // 构建 DFA 树
  private buildDfa(words: string[]) {
    this.root = { isEnd: false, children: new Map() }
    for (const word of words) {
      if (!word) continue
      let node = this.root
      for (const ch of word) {
        if (!node.children.has(ch)) {
          node.children.set(ch, { isEnd: false, children: new Map() })
        }
        node = node.children.get(ch)!
      }
      node.isEnd = true
    }
  }

  // 检测文本是否包含敏感词
  contains(text: string): boolean {
    if (!text) return false
    for (let i = 0; i < text.length; i++) {
      let node = this.root
      let j = i
      while (j < text.length && node.children.has(text[j])) {
        node = node.children.get(text[j])!
        if (node.isEnd) return true
        j++
      }
    }
    return false
  }

  // 替换敏感词为 *
  replace(text: string, replaceChar = '*'): string {
    if (!text) return text
    let result = text
    for (let i = 0; i < text.length; i++) {
      let node = this.root
      let j = i
      let found = false
      while (j < text.length && node.children.has(text[j])) {
        node = node.children.get(text[j])!
        if (node.isEnd) {
          found = true
          break
        }
        j++
      }
      if (found) {
        const stars = replaceChar.repeat(j - i + 1)
        result = result.slice(0, i) + stars + result.slice(j + 1)
        i = j // 跳过已替换部分
      }
    }
    return result
  }

  // 获取匹配到的第一个敏感词
  findFirst(text: string): string | null {
    for (let i = 0; i < text.length; i++) {
      let node = this.root
      let j = i
      while (j < text.length && node.children.has(text[j])) {
        node = node.children.get(text[j])!
        if (node.isEnd) {
          return text.substring(i, j + 1)
        }
        j++
      }
    }
    return null
  }
}

// 单例
const sensitiveFilter = new SensitiveFilter()

// 初始化（在应用启动时调用）
export async function initSensitiveFilter() {
  await sensitiveFilter.loadWords()
}

// 确保词库已加载
async function ensureLoaded() {
  if (!(sensitiveFilter as any).isLoaded) {
    await sensitiveFilter.loadWords()
  }
}

// 对外 API：异步检测是否包含敏感词
export async function containsSensitive(text: string): Promise<boolean> {
  await ensureLoaded()
  return sensitiveFilter.contains(text)
}

// 对外 API：异步替换敏感词
export async function filterSensitive(text: string, replaceChar = '*'): Promise<string> {
  await ensureLoaded()
  return sensitiveFilter.replace(text, replaceChar)
}

// 对外 API：异步获取第一个敏感词
export async function findFirstSensitive(text: string): Promise<string | null> {
  await ensureLoaded()
  return sensitiveFilter.findFirst(text)
}