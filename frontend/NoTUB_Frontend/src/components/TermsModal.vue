<template>
  <q-dialog :model-value="modelValue" @update:model-value="emit('update:modelValue', $event)" maximized transition-show="slide-up" transition-hide="slide-down">
    <div class="terms-modal">
      <div class="terms-modal__header">
        <q-btn flat dense icon="close" color="dark" @click="close" class="close-btn" />
        <h1 class="terms-modal__title">{{ title }}</h1>
      </div>

      <div class="terms-modal__body">
        <slot>
          <section class="terms-section" v-if="section === 'terms' || section === 'all'">
            <h2>1. Disposicoes Gerais</h2>
            <p>Os presentes Termos e Condicoes regulam a utilizacao da aplicacao NoTUB - Gestao de Informacao de Transporte Publico, doravante designada por "NoTUB", propriedade da Equipa NoTUB.</p>
            <p>A utilizacao da aplicacao implica a aceitacao integral dos presentes Termos e Condicoes. Caso nao concorde com algum dos termos aqui apresentados, nao devera utilizar a aplicacao.</p>

            <h2>2. Registo de Utilizador</h2>
            <p>Para utilizar determinadas funcionalidades do NoTUB, e necessario criar uma conta pessoal. O utilizador compromete-se a fornecer dados verdadeiros, exatos e completos durante o processo de registo.</p>
            <p>O utilizador e responsavel por manter a confidencialidade das suas credenciais de acesso, nomeadamente a palavra-passe, assumindo toda a responsabilidade por qualquer utilizacao indevida da sua conta.</p>

            <h2>3. Servicos Prestados</h2>
            <p>O NoTUB disponibiliza os seguintes servicos:</p>
            <ul>
              <li>Compra de bilhetes e passes de transporte;</li>
              <li>Consulta de informacao sobre rotas, horarios e paragens;</li>
              <li>Sistema de acumulacao e resgate de pontos;</li>
              <li>Validacao de titulos de transporte via codigo QR;</li>
              <li>Gestao de dados pessoais e preferencias de conta.</li>
            </ul>

            <h2>4. Pagamentos</h2>
            <p>Os pagamentos na aplicacao sao processados atraves de metodos seguros, incluindo cartao de credito/debito via Stripe.</p>
            <p>Todas as transacoes financeiras sao encriptadas e processadas por entidades certificadas. O NoTUB nao armazena dados bancarios dos utilizadores.</p>
            <p>Em caso de erro na transacao, o utilizador devera contactar o apoio ao cliente para resolucao do incidente.</p>

            <h2>5. Politica de Reembolso</h2>
            <p>Os bilhetes comprados sao nao reembolsaveis apos a sua validacao. Passes mensais ou anuais poderao ser objeto de reembolso parcial, proporcional ao periodo nao utilizado, mediante solicitacao escrita.</p>

            <h2>6. Pontos e Recompensas</h2>
            <p>O programa de pontos do NoTUB permite ao utilizador acumular pontos apenas quando a viagem e explicitamente terminada na aplicacao. Viagens nao encerradas ou concluídas fora do prazo máximo de 24 horas nao geram pontos.</p>
            <p>Os pontos podem ser trocados por bilhetes gratuitos, de acordo com as condicoes vigentes.</p>
            <p>Os pontos acumulados nao sao transferiveis, nao possuem valor monetario e podem expirar apos 12 meses de inatividade na conta.</p>

            <h2>7. Codigo de Conduta</h2>
            <p>O utilizador compromete-se a utilizar a aplicacao de forma responsavel e licita, abstraindo-se de:</p>
            <ul>
              <li>Utilizar a aplicacao para fins ilicitos ou nao autorizados;</li>
              <li>Fornecer informacao falsa ou enganosa;</li>
              <li>Tentar aceder indevidamente a contas de terceiros;</li>
              <li>Violar os direitos de propriedade intelectual do NoTUB.</li>
            </ul>

            <h2>8. Limitacao de Responsabilidade</h2>
            <p>O NoTUB nao se responsabiliza por interrupcoes no servico causadas por motivos de forca maior, incluindo mas nao limitado a falhas tecnicas, manutencao programada ou circunstancias externas.</p>
            <p>A informacao sobre horarios e rotas e fornecida a titulo indicativo, podendo sofrer alteracoes sem aviso previo.</p>

            <h2>9. Alteracoes aos Termos</h2>
            <p>O NoTUB reserva-se o direito de alterar os presentes Termos e Condicoes a qualquer momento. As alteracoes serao comunicadas aos utilizadores atraves da aplicacao ou por email.</p>
            <p>A continuacao da utilizacao da aplicacao apos a publicacao das alteracoes implica a aceitacao das mesmas.</p>

            <h2>10. Lei Aplicavel</h2>
            <p>Os presentes Termos e Condicoes sao regidos pela lei portuguesa. Para a resolucao de qualquer litigio, fica eleito o foro da Comarca de Braga, com renuncia expressa a qualquer outro.</p>

            <h2>11. Contacto</h2>
            <p>Para questoes relacionadas com estes Termos e Condicoes ou com o servico NoTUB, pode contactar-nos atraves de:</p>
            <p><strong>Email:</strong> apoio@notub.pt</p>
          </section>

          <section class="terms-section" v-if="section === 'privacy' || section === 'all'">
            <h2>Politica de Privacidade</h2>
            <p>O NoTUB compromete-se a proteger os dados pessoais dos utilizadores em conformidade com o Regulamento Geral sobre a Protecao de Dados (RGPD).</p>
            <p>Os dados pessoais recolhidos sao utilizados exclusivamente para:</p>
            <ul>
              <li>Gestao da conta do utilizador;</li>
              <li>Processamento de transacoes;</li>
              <li>Comunicacoes relacionadas com o servico;</li>
              <li>Melhoria continua da aplicacao.</li>
            </ul>
            <p>O utilizador pode exercer os seus direitos de acesso, retificacao, eliminacao e portabilidade dos dados, contactando a equipa NoTUB.</p>
            <p><strong>Email:</strong> apoio@notub.pt</p>
          </section>
        </slot>
      </div>
    </div>
  </q-dialog>
</template>

<script setup>
defineProps({
  modelValue: Boolean,
  section: { type: String, default: 'terms' },
  title: { type: String, default: 'Termos e Condicoes' }
})

const emit = defineEmits(['update:modelValue'])

function close() {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.terms-modal {
  background: #f8f9fa;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.terms-modal__header {
  padding: 12px 18px;
  background: #fff;
  border-bottom: 1px solid #e9ecef;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.close-btn {
  padding: 0;
}

.terms-modal__title {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 800;
  color: #0b1a16;
  margin: 0;
}

.terms-modal__body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 18px;
}

.terms-section h2 {
  font-family: 'Inter', sans-serif;
  font-size: 15px;
  font-weight: 700;
  color: #0b1a16;
  margin: 20px 0 8px 0;
}

.terms-section h2:first-child {
  margin-top: 0;
}

.terms-section p {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #495057;
  line-height: 1.6;
  margin: 0 0 8px 0;
}

.terms-section ul {
  padding-left: 20px;
  margin: 0 0 8px 0;
}

.terms-section li {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  color: #495057;
  line-height: 1.6;
  margin-bottom: 4px;
}
</style>
