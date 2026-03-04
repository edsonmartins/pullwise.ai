import { cn } from '@/lib/utils'

interface FileCoverage {
  filePath: string
  totalLinesChanged: number
  linesReviewed: number
  coveragePercentage: number
}

interface CoverageBarProps {
  averageCoverage: number
  files: FileCoverage[]
  showFiles?: boolean
  className?: string
}

function getCoverageColor(percentage: number): string {
  if (percentage >= 80) return 'bg-green-500'
  if (percentage >= 50) return 'bg-yellow-500'
  return 'bg-red-500'
}

function getCoverageTextColor(percentage: number): string {
  if (percentage >= 80) return 'text-green-600 dark:text-green-400'
  if (percentage >= 50) return 'text-yellow-600 dark:text-yellow-400'
  return 'text-red-600 dark:text-red-400'
}

export function CoverageBar({ averageCoverage, files, showFiles = false, className }: CoverageBarProps) {
  return (
    <div className={cn('space-y-3', className)}>
      {/* Overall coverage */}
      <div className="space-y-1">
        <div className="flex items-center justify-between text-sm">
          <span className="font-medium text-muted-foreground">Review Coverage</span>
          <span className={cn('font-semibold', getCoverageTextColor(averageCoverage))}>
            {averageCoverage.toFixed(1)}%
          </span>
        </div>
        <div className="h-2 w-full rounded-full bg-muted overflow-hidden">
          <div
            className={cn('h-full rounded-full transition-all duration-500', getCoverageColor(averageCoverage))}
            style={{ width: `${Math.min(averageCoverage, 100)}%` }}
          />
        </div>
      </div>

      {/* Per-file coverage */}
      {showFiles && files.length > 0 && (
        <div className="space-y-2 pl-2 border-l-2 border-muted">
          {files
            .sort((a, b) => a.coveragePercentage - b.coveragePercentage)
            .map((file) => (
              <div key={file.filePath} className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-muted-foreground truncate max-w-[70%]" title={file.filePath}>
                    {file.filePath.split('/').pop()}
                  </span>
                  <span className={cn('font-mono', getCoverageTextColor(file.coveragePercentage))}>
                    {file.linesReviewed}/{file.totalLinesChanged} ({file.coveragePercentage.toFixed(0)}%)
                  </span>
                </div>
                <div className="h-1 w-full rounded-full bg-muted overflow-hidden">
                  <div
                    className={cn('h-full rounded-full', getCoverageColor(file.coveragePercentage))}
                    style={{ width: `${Math.min(file.coveragePercentage, 100)}%` }}
                  />
                </div>
              </div>
            ))}
        </div>
      )}
    </div>
  )
}
