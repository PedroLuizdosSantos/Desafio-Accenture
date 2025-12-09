# --- Imports e configurações básicas ---
from flask import Flask, render_template, request, redirect, url_for, flash
import requests
import os

# URL da API Java (backend). Em produção vem de variável de ambiente, senão usa padrão.
API_BASE = os.getenv("API_BASE", "http://backend:8081")

# API pública de CEP
CEP_API_BASE = "http://cep.la"

app = Flask(__name__)
app.secret_key = "segredo-super-simples"  # usado pelo flash e sessões


# --- ROTA INICIAL: só redireciona para a listagem de empresas ---
@app.route("/")
def index():
    return redirect(url_for("listar_empresas"))


# --- FUNÇÃO AUXILIAR: valida e consulta CEP em duas APIs diferentes ---
def validar_cep(cep: str):
    # normaliza o CEP tirando pontos e traços
    cep = (cep or "").strip().replace(".", "").replace("-", "")

    # validação básica: 8 dígitos numéricos
    if len(cep) != 8 or not cep.isdigit():
        return False, "CEP deve ter exatamente 8 dígitos.", None

    # 1ª tentativa: API cep.la
    try:
        resp = requests.get(
            f"{CEP_API_BASE}/{cep}",
            headers={"Accept": "application/json"},
            timeout=2
        )
        if resp.status_code == 200:
            data = resp.json()
            if data:
                return True, None, {
                    "uf": data.get("uf"),
                    "localidade": data.get("cidade"),
                    "bairro": data.get("bairro")
                }
    except Exception:
        pass  # se falhar, tenta a próxima API

    # 2ª tentativa: ViaCEP (API alternativa/teste)
    try:
        resp = requests.get(f"https://viacep.com.br/ws/{cep}/json/", timeout=2)
        if resp.status_code == 200:
            data = resp.json()
            if data.get("erro"):
                return False, "CEP inválido.", None
            return True, "CEP validado via ViaCEP (API alternativa de teste).", data

        # se a API responder algo estranho, considera CEP ok mas sem detalhes
        return True, "Impossível validar CEP (APIs offline).", None
    except Exception:
        # se der erro geral de rede, também não trava o fluxo
        return True, "Erro ao validar CEP (APIs offline).", None


# --- FUNÇÕES AUXILIARES DE ACESSO À API BACKEND (EMPRESAS/FORNECEDORES) ---
def get_empresas():
    resp = requests.get(f"{API_BASE}/empresas")
    resp.raise_for_status()
    return resp.json()


def get_empresa(id_):
    resp = requests.get(f"{API_BASE}/empresas/{id_}")
    if resp.status_code == 200:
        return resp.json()
    return None


def get_fornecedores():
    resp = requests.get(f"{API_BASE}/fornecedores")
    resp.raise_for_status()
    return resp.json()


def get_fornecedor(id_):
    resp = requests.get(f"{API_BASE}/fornecedores/{id_}")
    if resp.status_code == 200:
        return resp.json()
    return None

#   ROTAS DE EMPRESAS

# LISTAR + CRIAR EMPRESA (GET/POST na mesma rota)
@app.route("/empresas", methods=["GET", "POST"])
def listar_empresas():
    # POST: criação de nova empresa via formulário
    if request.method == "POST":
        cnpj = request.form.get("cnpj")
        nome = request.form.get("nomeFantasia")
        cep = request.form.get("cep")
        estado = request.form.get("estado")
        cidade = request.form.get("cidade")
        bairro = request.form.get("bairro")

        # valida CEP antes de mandar pro backend
        valido, msg, cep_data = validar_cep(cep)

        if not valido:
            flash(msg, "erro")
            return redirect(url_for("listar_empresas"))

        # se a API de CEP trouxe dados, preenche/ajusta estado, cidade e bairro
        if cep_data:
            estado = cep_data.get("uf", estado)
            cidade = cep_data.get("localidade", cidade)
            bairro = cep_data.get("bairro", bairro)

        # corpo que vai para a API Java
        body = {
            "cnpj": cnpj,
            "nomeFantasia": nome,
            "cep": cep,
            "estado": estado.upper() if estado else None
        }

        try:
            resp = requests.post(f"{API_BASE}/empresas", json=body)
            if resp.status_code in (200, 201):
                flash("Empresa criada com sucesso!", "ok")
                return redirect(url_for("listar_empresas"))
            else:
                # se backend mandou mensagem de erro, mostra na tela
                erro_backend = resp.text.strip() or f"status {resp.status_code}"
                flash(f"Erro ao criar empresa: {erro_backend}", "erro")
        except Exception:
            flash("Erro ao criar empresa (falha de comunicação com o backend).", "erro")

    # GET: apenas carrega a lista de empresas para exibir
    try:
        empresas = get_empresas()
    except Exception:
        empresas = []
        flash("Erro ao carregar empresas.", "erro")

    return render_template("empresas.html", empresas=empresas)


# EDITAR EMPRESA (carrega form + salva alterações)
@app.route("/empresas/<int:empresa_id>/editar", methods=["GET", "POST"])
def editar_empresa(empresa_id):
    if request.method == "POST":
        cnpj = request.form.get("cnpj")
        nomeFantasia = request.form.get("nomeFantasia")
        cep = request.form.get("cep")
        estado = request.form.get("estado")

        # valida CEP novamente na edição
        valido, msg, _ = validar_cep(cep)

        if not valido:
            flash(msg, "erro")
            return redirect(url_for("editar_empresa", empresa_id=empresa_id))

        body = {
            "cnpj": cnpj,
            "nomeFantasia": nomeFantasia,
            "cep": cep,
            "estado": estado,
        }

        try:
            resp = requests.put(f"{API_BASE}/empresas/{empresa_id}", json=body)
            if resp.status_code in (200, 204):
                flash("Empresa atualizada com sucesso!", "ok")
                return redirect(url_for("listar_empresas"))
            else:
                flash("Erro ao atualizar empresa.", "erro")
        except Exception:
            flash("Erro de comunicação com o backend ao atualizar empresa.", "erro")

    # GET: busca empresa específica para preencher o formulário
    resp = requests.get(f"{API_BASE}/empresas/{empresa_id}")
    empresa = resp.json()
    return render_template("empresa_form.html", empresa=empresa)


# DELETAR EMPRESA
@app.route("/empresas/<int:empresa_id>/deletar", methods=["POST"])
def deletar_empresa(empresa_id):
    try:
        resp = requests.delete(f"{API_BASE}/empresas/{empresa_id}")
        if resp.status_code in (200, 204):
            flash("Empresa excluída com sucesso!", "ok")
        elif resp.status_code == 404:
            flash("Empresa não encontrada.", "erro")
        else:
            erro_backend = resp.text.strip() or f"status {resp.status_code}"
            flash(f"Erro ao excluir empresa: {erro_backend}", "erro")
    except Exception:
        flash("Erro ao excluir empresa (falha de comunicação com o backend).", "erro")

    return redirect(url_for("listar_empresas"))

#   ROTAS DE FORNECEDORES

# LISTAR + CRIAR FORNECEDOR
@app.route("/fornecedores", methods=["GET", "POST"])
def listar_fornecedores():
    # POST: criação de fornecedor
    if request.method == "POST":
        nome = request.form.get("nome")
        cpfCnpj = request.form.get("cpfCnpj")
        email = request.form.get("email")
        rg = request.form.get("rg")
        dataNascimento = request.form.get("dataNascimento")
        cep = request.form.get("cep")
        tipo = request.form.get("tipoPessoa")

        valido, msg, cep_data = validar_cep(cep)

        if not valido:
            flash(msg, "erro")
            return redirect(url_for("listar_fornecedores"))

        body = {
            "nome": nome,
            "cpfCnpj": cpfCnpj,
            "email": email,
            "rg": rg,
            "dataNascimento": dataNascimento,
            "cep": cep,
            "tipoPessoa": tipo
        }

        try:
            resp = requests.post(f"{API_BASE}/fornecedores", json=body)
            if resp.status_code in (200, 201):
                flash("Fornecedor criado com sucesso!", "ok")
                return redirect(url_for("listar_fornecedores"))
            else:
                erro_backend = resp.text.strip() or f"status {resp.status_code}"
                flash(f"Erro ao salvar fornecedor ({erro_backend}).", "erro")
        except Exception:
            flash("Erro de comunicação com o backend ao salvar fornecedor.", "erro")

    # Filtros da tela (nome e CPF/CNPJ)
    filtro_nome = (request.args.get("nome") or "").lower()
    filtro_cpf = (request.args.get("cpfCnpj") or "").lower()

    # Carrega todos os fornecedores do backend
    try:
        fornecedores = get_fornecedores()
    except Exception:
        fornecedores = []
        flash("Erro ao carregar fornecedores.", "erro")

    # Função interna para aplicar os filtros na lista
    def passa(f):
        if filtro_nome and filtro_nome not in f["nome"].lower():
            return False
        if filtro_cpf and filtro_cpf not in f["cpfCnpj"].lower():
            return False
        return True

    fornecedores_filtrados = [f for f in fornecedores if passa(f)]

    return render_template(
        "fornecedores.html",
        fornecedores=fornecedores_filtrados,
        filtro_nome=filtro_nome,
        filtro_cpf=filtro_cpf
    )


# EDITAR FORNECEDOR
@app.route("/fornecedores/<int:fornecedor_id>/editar", methods=["GET", "POST"])
def editar_fornecedor(fornecedor_id):
    if request.method == "POST":
        nome = request.form.get("nome")
        cpfCnpj = request.form.get("cpfCnpj")
        email = request.form.get("email")
        rg = request.form.get("rg")
        dataNascimento = request.form.get("dataNascimento")
        cep = request.form.get("cep")
        estado = request.form.get("estado")
        tipoPessoa = request.form.get("tipoPessoa")

        valido, msg, _ = validar_cep(cep)

        if not valido:
            flash(msg, "erro")
            return redirect(url_for("editar_fornecedor", fornecedor_id=fornecedor_id))

        body = {
            "nome": nome,
            "cpfCnpj": cpfCnpj,
            "email": email,
            "rg": rg,
            "dataNascimento": dataNascimento,
            "cep": cep,
            "estado": estado,
            "tipoPessoa": tipoPessoa,
        }

        try:
            resp = requests.put(f"{API_BASE}/fornecedores/{fornecedor_id}", json=body)
            if resp.status_code in (200, 204):
                flash("Fornecedor atualizado com sucesso!", "ok")
                return redirect(url_for("listar_fornecedores"))
            else:
                flash("Erro ao atualizar fornecedor.", "erro")
        except Exception:
            flash("Erro de comunicação com o backend ao atualizar fornecedor.", "erro")

    # GET: busca fornecedor para preencher formulário de edição
    resp = requests.get(f"{API_BASE}/fornecedores/{fornecedor_id}")
    fornecedor = resp.json()
    return render_template("fornecedor_form.html", fornecedor=fornecedor)


# DELETAR FORNECEDOR
@app.route("/fornecedores/<int:fornecedor_id>/deletar", methods=["POST"])
def deletar_fornecedor(fornecedor_id):
    try:
        resp = requests.delete(f"{API_BASE}/fornecedores/{fornecedor_id}")
        if resp.status_code in (200, 204):
            flash("Fornecedor excluído com sucesso!", "ok")
        elif resp.status_code == 404:
            flash("Fornecedor não encontrado.", "erro")
        else:
            erro_backend = resp.text.strip() or f"status {resp.status_code}"
            flash(f"Erro ao excluir fornecedor: {erro_backend}", "erro")
    except Exception:
        flash("Erro ao excluir fornecedor (falha de comunicação com o backend).", "erro")

    return redirect(url_for("listar_fornecedores"))


#   ROTAS DE VÍNCULOS EMPRESA x FORNECEDOR

@app.route("/vinculos", methods=["GET", "POST"])
def vinculos():
    # listas usadas para montar os selects e a tabela na tela
    empresas = []
    fornecedores = []
    fornecedores_empresa = []
    empresa_id = request.args.get("empresa_id")

    # carrega empresas e fornecedores para popular os selects
    try:
        empresas = get_empresas()
        fornecedores = get_fornecedores()
    except Exception:
        flash("Erro ao carregar empresas ou fornecedores.", "erro")

    # POST: cria vínculo entre uma empresa e um fornecedor
    if request.method == "POST":
        emp = request.form.get("empresa_id")
        forn = request.form.get("fornecedor_id")

        try:
            resp = requests.post(f"{API_BASE}/empresas/{emp}/fornecedores/{forn}")
            if resp.status_code in (200, 201):
                flash("Vínculo criado com sucesso!", "ok")
                return redirect(url_for("vinculos", empresa_id=emp))
            else:
                erro_backend = resp.text.strip() or f"status {resp.status_code}"
                flash(f"Erro ao criar vínculo: {erro_backend}", "erro")
        except Exception:
            flash("Erro ao criar vínculo (falha de comunicação com o backend).", "erro")

    # se alguma empresa foi selecionada, carrega os fornecedores já vinculados a ela
    if empresa_id:
        try:
            r = requests.get(f"{API_BASE}/empresas/{empresa_id}/fornecedores")
            if r.status_code == 200:
                fornecedores_empresa = r.json()
        except Exception:
            flash("Erro ao carregar fornecedores da empresa.", "erro")

    return render_template(
        "vinculos.html",
        empresas=empresas,
        fornecedores=fornecedores,
        fornecedores_da_empresa=fornecedores_empresa,
        empresa_id_selecionada=empresa_id
    )


# DESVINCULAR EMPRESA x FORNECEDOR
@app.route("/vinculos/desvincular", methods=["POST"])
def desvincular_vinculo():
    empresa_id = request.form.get("empresa_id")
    fornecedor_id = request.form.get("fornecedor_id")

    if not empresa_id or not fornecedor_id:
        flash("Dados inválidos para desvincular.", "erro")
        return redirect(url_for("vinculos"))

    try:
        resp = requests.delete(f"{API_BASE}/empresas/{empresa_id}/fornecedores/{fornecedor_id}")
        if resp.status_code in (200, 204):
            flash("Vínculo removido com sucesso!", "ok")
        elif resp.status_code == 400:
            flash(resp.text or "Não foi possível desvincular.", "erro")
        elif resp.status_code == 404:
            flash("Empresa ou fornecedor não encontrado, ou vínculo inexistente.", "erro")
        else:
            flash(f"Erro ao desvincular: {resp.text}", "erro")
    except Exception:
        flash("Erro ao desvincular (falha de comunicação com o backend).", "erro")

    return redirect(url_for("vinculos", empresa_id=empresa_id))


# --- PONTO DE ENTRADA DA APLICAÇÃO FLASK (modo standalone) ---
if __name__ == "__main__":
    app.run(
        host="0.0.0.0",  # acessível de fora do container/maquina
        port=5000,
        debug=True       
    )
