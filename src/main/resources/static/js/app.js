// JavaScript customizado
document.addEventListener('DOMContentLoaded', function() {
    console.log('Página carregada!');
    
    // Exemplo de interação
    const buttons = document.querySelectorAll('.btn-primary');
    buttons.forEach(button => {
        button.addEventListener('click', function() {
            console.log('Botão clicado:', this.textContent);
        });
    });
});