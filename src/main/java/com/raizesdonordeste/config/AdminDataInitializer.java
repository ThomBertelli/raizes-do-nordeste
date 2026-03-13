package com.raizesdonordeste.config;

import com.raizesdonordeste.domain.enums.PerfilUsuario;
import com.raizesdonordeste.domain.model.Usuario;
import com.raizesdonordeste.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.nome}")
    private String adminNome;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.senha}")
    private String adminSenha;

    @Override
    @Transactional
    @SuppressWarnings("NullableProblems")
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByPerfil(PerfilUsuario.ADMIN)) {
            log.info("Admin já existe. Nenhum seed necessário.");
            return;
        }

        Usuario admin = Usuario.builder()
                .nome(adminNome)
                .email(adminEmail)
                .senha(passwordEncoder.encode(adminSenha))
                .perfil(PerfilUsuario.ADMIN)
                .ativo(true)
                .consentimentoProgramaFidelidade(false)
                .build();

        usuarioRepository.save(admin);
        log.info("Admin inicial criado: {}", adminEmail);
    }
}





