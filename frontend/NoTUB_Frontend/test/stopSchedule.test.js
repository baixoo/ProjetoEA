import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildDisplayRoutes,
  extractLineCode,
  extractTerminals,
  pickBestTerminal,
  formatWait,
  hasDelayInfo,
  hasOccupancy,
  occupancyLabel
} from '../src/utils/stopSchedule.js'

// ── extractLineCode ────────────────────────────────────────────────────────────
test('extractLineCode returns the code before the first dash', () => {
  assert.equal(extractLineCode('9M - Aliados - Gondomar (via Tic)'), '9M')
  assert.equal(extractLineCode('205 - Campanhã - Castelo Do Queijo'), '205')
  assert.equal(extractLineCode('2'), '2')
  assert.equal(extractLineCode(''), '')
})

// ── extractTerminals ───────────────────────────────────────────────────────────
test('extractTerminals returns the two endpoint names from a full line name', () => {
  assert.deepEqual(
    extractTerminals('9M - Aliados - Gondomar (via Tic)'),
    ['Aliados', 'Gondomar (via Tic)']
  )
  assert.deepEqual(
    extractTerminals('205 - Campanhã - Castelo Do Queijo'),
    ['Campanhã', 'Castelo Do Queijo']
  )
  assert.deepEqual(extractTerminals('2'), [])
  assert.deepEqual(extractTerminals(''), [])
})

// ── pickBestTerminal ───────────────────────────────────────────────────────────
test('pickBestTerminal selects the terminal that matches destinoFinal', () => {
  assert.equal(
    pickBestTerminal(['Aliados', 'Gondomar (via Tic)'], 'Gondomar (Souto)'),
    'Gondomar (via Tic)'
  )
  assert.equal(
    pickBestTerminal(['Aliados', 'Gondomar (via Tic)'], 'Aliados'),
    'Aliados'
  )
  assert.equal(pickBestTerminal([], 'Qualquer Sítio'), 'Qualquer Sítio')
})

// ── buildDisplayRoutes ─────────────────────────────────────────────────────────
test('buildDisplayRoutes flattens every direction and sorts line names numerically', () => {
  const routes = buildDisplayRoutes({
    linhas: [
      {
        linhaId: 10,
        linhaNome: '10',
        trajetos: [{ trajetoId: 100, destinoFinal: 'Centro', proximosPasses: [] }]
      },
      {
        linhaId: 2,
        linhaNome: '2',
        trajetos: [
          { trajetoId: 202, destinoFinal: 'Universidade', proximosPasses: [] },
          { trajetoId: 201, destinoFinal: 'Aeroporto', proximosPasses: [] }
        ]
      }
    ]
  })

  assert.deepEqual(
    routes.map(route => ({ key: route.key, linhaNome: route.linhaNome, destinoFinal: route.destinoFinal })),
    [
      { key: '2-201', linhaNome: '2', destinoFinal: 'Aeroporto' },
      { key: '2-202', linhaNome: '2', destinoFinal: 'Universidade' },
      { key: '10-100', linhaNome: '10', destinoFinal: 'Centro' }
    ]
  )
})

test('buildDisplayRoutes tolerates schedules without route arrays', () => {
  assert.deepEqual(buildDisplayRoutes(null), [])
  assert.deepEqual(buildDisplayRoutes({ linhas: [{ linhaId: 1, linhaNome: '1' }] }), [])
})

test('buildDisplayRoutes shows code + direction-specific terminal from linhaNome', () => {
  const routes = buildDisplayRoutes({
    linhas: [
      {
        linhaId: 73,
        linhaNome: '9M - Aliados - Gondomar (via Tic)',
        trajetos: [
          { trajetoId: 145, destinoFinal: 'Gondomar (Souto)', proximosPasses: [] },
          { trajetoId: 146, destinoFinal: 'Aliados', proximosPasses: [] }
        ]
      },
      {
        linhaId: 15,
        linhaNome: '205 - Campanhã - Castelo Do Queijo',
        trajetos: [
          { trajetoId: 29, destinoFinal: 'Castelo Do Queijo III', proximosPasses: [] },
          { trajetoId: 30, destinoFinal: 'Campanhã II', proximosPasses: [] }
        ]
      }
    ]
  })

  assert.deepEqual(
    routes.map(route => route.displayName),
    [
      '9M - Aliados',
      '9M - Gondomar (via Tic)',
      '205 - Campanhã',
      '205 - Castelo Do Queijo'
    ]
  )
})

// ── occupancy helpers ──────────────────────────────────────────────────────────
test('occupancy helpers distinguish available and unavailable capacity data', () => {
  const available = { lotacaoAtual: 12, nLugares: 40 }
  assert.equal(hasOccupancy(available), true)
  assert.equal(occupancyLabel(available), '12/40 lugares')
  assert.equal(hasOccupancy({ lotacaoAtual: 12 }), false)
  assert.equal(occupancyLabel({ lotacaoAtual: 12 }), 'Lotação indisponível')
})

// ── wait / delay helpers ───────────────────────────────────────────────────────
test('wait and delay helpers cover arriving, delayed, and unavailable states', () => {
  assert.equal(formatWait(0), 'A chegar')
  assert.equal(formatWait(-2), 'A chegar')
  assert.equal(formatWait(7), '7 min')
  assert.equal(hasDelayInfo({ tempoAtraso: 0 }), true)
  assert.equal(hasDelayInfo({ tempoAtraso: 4 }), true)
  assert.equal(hasDelayInfo({}), false)
})
