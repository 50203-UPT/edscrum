package pt.up.edscrum.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import pt.up.edscrum.repository.UserRepository;

@ControllerAdvice
/**
 * Define atributos globais de modelo usados pelas views (ex.: mapa de
 * imagens de perfil dos utilizadores).
 */
public class GlobalModelAttributes {

    @Autowired
    private UserRepository userRepository;

    @ModelAttribute("userProfileImageMap")
    public Map<Long, String> userProfileImageMap() {
        Map<Long, String> map = new HashMap<>();
        userRepository.findAll().forEach(u -> map.put(u.getId(), u.getProfileImage()));
        return map;
    }
}
