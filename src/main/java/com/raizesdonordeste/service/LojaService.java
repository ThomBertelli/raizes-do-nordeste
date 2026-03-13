package com.raizesdonordeste.service;

import com.raizesdonordeste.api.dto.loja.LojaAtualizacaoDTO;
import com.raizesdonordeste.api.dto.loja.LojaCriacaoDTO;
import com.raizesdonordeste.api.dto.loja.LojaRespostaDTO;
import com.raizesdonordeste.domain.model.Loja;
import com.raizesdonordeste.domain.repository.LojaRepository;
import com.raizesdonordeste.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LojaService {

    private final LojaRepository lojaRepository;

    @Transactional
    public LojaRespostaDTO criar(LojaCriacaoDTO dto) {
        if (lojaRepository.existsByCnpj(dto.getCnpj())) {
            throw new IllegalArgumentException("CNPJ já cadastrado");
        }

        Loja loja = Loja.builder()
                .nome(dto.getNome())
                .cnpj(dto.getCnpj())
                .endereco(dto.getEndereco())
                .ativa(true)
                .build();

        return toDTO(lojaRepository.save(loja));
    }

    @Transactional
    public LojaRespostaDTO atualizar(Long id, LojaAtualizacaoDTO dto) {
        Loja loja = buscarEntidade(id);

        if (dto.getNome() != null) {
            loja.setNome(dto.getNome());
        }

        if (dto.getCnpj() != null && !dto.getCnpj().equals(loja.getCnpj())) {
            if (lojaRepository.existsByCnpj(dto.getCnpj())) {
                throw new IllegalArgumentException("CNPJ já cadastrado");
            }
            loja.setCnpj(dto.getCnpj());
        }

        if (dto.getEndereco() != null) {
            loja.setEndereco(dto.getEndereco());
        }

        if (dto.getAtiva() != null) {
            loja.setAtiva(dto.getAtiva());
        }

        return toDTO(lojaRepository.save(loja));
    }

    @Transactional(readOnly = true)
    public LojaRespostaDTO buscarPorId(Long id) {
        return toDTO(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public Page<LojaRespostaDTO> listarTodos(Pageable pageable) {
        return lojaRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LojaRespostaDTO> buscarAtivas(Pageable pageable) {
        return lojaRepository.findByAtiva(true, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public Page<LojaRespostaDTO> buscarPorNome(String nome, Pageable pageable) {
        return lojaRepository.findByNomeContainingIgnoreCase(nome, pageable).map(this::toDTO);
    }

    @Transactional
    public void ativar(Long id) {
        Loja loja = buscarEntidade(id);
        loja.setAtiva(true);
        lojaRepository.save(loja);
    }

    @Transactional
    public void desativar(Long id) {
        Loja loja = buscarEntidade(id);
        loja.setAtiva(false);
        lojaRepository.save(loja);
    }

    @Transactional
    public void deletar(Long id) {
        if (!lojaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Loja não encontrada");
        }
        lojaRepository.deleteById(id);
    }

    private Loja buscarEntidade(Long id) {
        return lojaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Loja não encontrada"));
    }

    private LojaRespostaDTO toDTO(Loja loja) {
        return LojaRespostaDTO.builder()
                .id(loja.getId())
                .nome(loja.getNome())
                .cnpj(loja.getCnpj())
                .endereco(loja.getEndereco())
                .ativa(loja.isAtiva())
                .dataCriacao(loja.getDataCriacao())
                .dataAtualizacao(loja.getDataAtualizacao())
                .build();
    }
}

