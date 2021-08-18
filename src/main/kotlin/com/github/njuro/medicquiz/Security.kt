package com.github.njuro.medicquiz

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager

@Configuration
@EnableWebSecurity
class Security : WebSecurityConfigurerAdapter() {

    @Value("\${app.password}")
    lateinit var password: String

    override fun configure(http: HttpSecurity) {
        http
            .authorizeRequests()
            .antMatchers("/test/**", "/static/**")
            .permitAll()
            .anyRequest()
            .authenticated()
            .and()
            .formLogin()
            .defaultSuccessUrl("/generate", true)
            .permitAll()
            .and()
            .logout()
            .permitAll()
            .and()
            .csrf()
            .disable()
    }

    @Bean
    override fun userDetailsService(): UserDetailsService? {
        val user: UserDetails = User.builder()
            .passwordEncoder { passwordEncoder().encode(it) }
            .username("admin")
            .password(password)
            .roles("ADMIN")
            .build()

        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }
}