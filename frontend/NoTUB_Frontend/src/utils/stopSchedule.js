export function buildDisplayRoutes(schedule) {
  if (!Array.isArray(schedule?.linhas)) return []

  return schedule.linhas
    .flatMap(linha => {
      if (!Array.isArray(linha.trajetos)) return []

      const lineCode = extractLineCode(linha.linhaNome)
      const lineTerminals = extractTerminals(linha.linhaNome)

      return linha.trajetos.map(trajeto => {
        const displayDestination = normalizeTerminalName(trajeto.destinoFinal)

        // Pick the terminal from linhaNome that best matches this trajeto's destinoFinal.
        // Falls back to displayDestination (actual last-stop name) when there are no
        // named terminals in linhaNome (e.g. linhaNome is just "2").
        const matchedTerminal = pickBestTerminal(lineTerminals, displayDestination)
        const displayName = lineCode
          ? `${lineCode} - ${matchedTerminal}`
          : matchedTerminal

        return {
          ...trajeto,
          key: `${linha.linhaId}-${trajeto.trajetoId}`,
          linhaNome: linha.linhaNome,
          lineCode,
          displayDestination,
          displayName
        }
      })
    })
    .sort((a, b) => {
      const lineOrder = a.lineCode.localeCompare(b.lineCode, 'pt', { numeric: true })
      return lineOrder || a.displayDestination.localeCompare(b.displayDestination, 'pt')
    })
}

/**
 * Extracts the short line code from the full name.
 * "9M - Aliados - Gondomar (via Tic)" → "9M"
 * "205 - Campanhã - Castelo Do Queijo" → "205"
 * "2" → "2"
 */
export function extractLineCode(lineName) {
  return String(lineName ?? '').split(/\s+-\s+/u, 1)[0].trim()
}

/**
 * Extracts the named terminals from a full line name.
 * "9M - Aliados - Gondomar (via Tic)" → ["Aliados", "Gondomar (via Tic)"]
 * "205 - Campanhã - Castelo Do Queijo" → ["Campanhã", "Castelo Do Queijo"]
 * "2" → []
 */
export function extractTerminals(lineName) {
  const parts = String(lineName ?? '').split(/\s+-\s+/u)
  return parts.length > 1 ? parts.slice(1).map(t => t.trim()) : []
}

/**
 * Finds the terminal from the line name that most closely matches the
 * trajeto's destinoFinal. Uses a simple normalised token-overlap score.
 * Falls back to rawDestination when terminals list is empty.
 */
export function pickBestTerminal(terminals, rawDestination) {
  if (!terminals.length) return rawDestination

  const norm = s =>
    String(s)
      .toLowerCase()
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/[^a-z0-9\s]/g, '')
      .trim()

  const destNorm = norm(rawDestination)

  let best = terminals[0]
  let bestScore = -1

  for (const terminal of terminals) {
    const tNorm = norm(terminal)

    // Score: ratio of shared characters to the length of the shorter string
    const shorter = tNorm.length < destNorm.length ? tNorm : destNorm
    const longer  = tNorm.length < destNorm.length ? destNorm : tNorm
    const shared  = [...shorter].filter(c => longer.includes(c)).length
    const score   = shorter.length > 0 ? shared / shorter.length : 0

    if (score > bestScore) {
      bestScore = score
      best = terminal
    }
  }

  return best
}

export function normalizeTerminalName(terminalName) {
  return String(terminalName ?? '').replace(/\s+[IVX]+$/u, '').trim()
}

export function hasOccupancy(passe) {
  return Number.isInteger(passe?.lotacaoAtual) && Number.isInteger(passe?.nLugares)
}

export function occupancyLabel(passe) {
  return hasOccupancy(passe)
    ? `${passe.lotacaoAtual}/${passe.nLugares} lugares`
    : 'Lotação indisponível'
}

export function hasDelayInfo(passe) {
  return Number.isInteger(passe?.tempoAtraso)
}

export function formatWait(minutes) {
  if (minutes <= 0) return 'A chegar'
  return `${minutes} min`
}
