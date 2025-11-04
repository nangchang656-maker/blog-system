import CryptoJS from 'crypto-js'

// AES密钥（与后端保持一致，建议放在环境变量中）
const SECRET_KEY = 'MyBlogSecretKey2025'

/**
 * AES加密
 * @param data 原始数据
 * @returns 加密后的字符串
 */
export const encrypt = (data: string): string => {
  return CryptoJS.AES.encrypt(data, SECRET_KEY).toString()
}

/**
 * AES解密
 * @param encrypted 加密的数据
 * @returns 解密后的字符串
 */
export const decrypt = (encrypted: string): string => {
  const bytes = CryptoJS.AES.decrypt(encrypted, SECRET_KEY)
  return bytes.toString(CryptoJS.enc.Utf8)
}
