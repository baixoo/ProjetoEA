package pt.notub.security;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import pt.notub.exception.AutenticacaoRequeridaException;
import pt.notub.exception.RecursoNaoEncontradoException;
import pt.notub.models.Utilizador;
import pt.notub.repositories.UtilizadorRepository;

@Component
public class AuthenticatedUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UtilizadorRepository utilizadorRepository;

    public AuthenticatedUserArgumentResolver(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(AuthenticatedUser.class) != null
                && parameter.getParameterType().equals(Utilizador.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserDetailsImpl userDetails)) {
            throw new AutenticacaoRequeridaException();
        }
        return utilizadorRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Utilizador nao encontrado"));
    }
}
