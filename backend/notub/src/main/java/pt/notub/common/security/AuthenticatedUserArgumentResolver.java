package pt.notub.common.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import pt.notub.common.exception.AutenticacaoRequeridaException;
import pt.notub.common.exception.RecursoNaoEncontradoException;
import pt.notub.user.entity.Utilizador;
import pt.notub.user.repository.UtilizadorRepository;

@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UtilizadorRepository utilizadorRepository;

    public AuthenticatedUserArgumentResolver(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(AuthenticatedUser.class) != null
                && parameter.getParameterType().equals(AuthenticatedUserContext.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new AutenticacaoRequeridaException();
        }
        Utilizador utilizador = utilizadorRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
        return new AuthenticatedUserContext(utilizador.getId(), utilizador.getEmail(), utilizador.getRole());
    }
}
