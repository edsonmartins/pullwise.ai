import { Command } from 'commander'
import chalk from 'chalk'
import { apiClient } from '../../lib/api/client'
import { logger } from '../../lib/utils/logger'
import { Spinner } from '../../lib/utils/spinner'

interface VerificationResult {
  valid: boolean
  message: string
  payloadHash: string | null
  attestedAt: string | null
}

interface AttestationDTO {
  id: number
  reviewId: number
  payloadHash: string
  algorithm: string
  keyId: string
  createdAt: string
}

export const verifyReviewCommand = new Command('verify')
  .description('Verify the cryptographic attestation of a review')
  .argument('<reviewId>', 'Review ID to verify')
  .action(async (reviewId) => {
    try {
      const spinner = new Spinner('Verifying review attestation...')

      const client = await apiClient.getClient()

      // Fetch attestation
      let attestation: AttestationDTO
      try {
        const attResponse = await client.get(`/api/reviews/${reviewId}/attestation`)
        attestation = attResponse.data
      } catch {
        spinner.fail('No attestation found for this review')
        process.exit(1)
        return
      }

      // Verify
      const verifyResponse = await client.post(`/api/reviews/${reviewId}/attestation/verify`)
      const result: VerificationResult = verifyResponse.data

      spinner.stop()

      if (result.valid) {
        logger.box(
          `
${chalk.green.bold('PASS')} — Attestation is valid

Review ID:    ${reviewId}
Hash:         ${attestation.payloadHash.substring(0, 32)}...
Algorithm:    ${attestation.algorithm}
Key ID:       ${attestation.keyId}
Attested at:  ${attestation.createdAt}

${chalk.green(result.message)}
          `.trim(),
          { title: 'Attestation Verification', color: 'green' }
        )
      } else {
        logger.box(
          `
${chalk.red.bold('FAIL')} — Attestation verification failed

Review ID:    ${reviewId}
Hash:         ${result.payloadHash || 'N/A'}

${chalk.red(result.message)}
          `.trim(),
          { title: 'Attestation Verification', color: 'red' }
        )
        process.exit(1)
      }
    } catch (error: any) {
      logger.error('Verification failed', error.message)
      process.exit(1)
    }
  })
