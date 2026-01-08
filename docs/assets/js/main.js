    (function() {
        "use strict";

        // ==========================================================
        // 1. FORCE PRELOADER OFF
        // ==========================================================
        // This ensures the site shows up even if images are missing
        setTimeout(function() {
            var preloader = document.querySelector('.preloader');
            if (preloader) {
                preloader.style.opacity = '0';
                setTimeout(function() {
                    preloader.style.display = 'none';
                }, 300);
            }
        }, 500); 

        /* ==========================================================
        2. STICKY NAVBAR & LOGO SWITCHER (SAFE MODE)
        ========================================================== */
        window.onscroll = function () {
            var header_navbar = document.querySelector(".navbar-area");
            var sticky = header_navbar ? header_navbar.offsetTop : 0;
            var logo = document.querySelector('.navbar-brand img'); // Tries to find image

            if (header_navbar) {
                if (window.pageYOffset > sticky) {
                    header_navbar.classList.add("sticky");
                    // Only try to swap logo if an image logo actually exists
                    if (logo) {
                        logo.src = 'assets/img/logo/logo-2.svg';
                    }
                } else {
                    header_navbar.classList.remove("sticky");
                    // Only try to swap logo if an image logo actually exists
                    if (logo) {
                        logo.src = 'assets/img/logo/logo.svg';
                    }
                }
            }

            // Back to top button logic
            var backToTop = document.querySelector(".scroll-top");
            if (backToTop) {
                if (document.body.scrollTop > 50 || document.documentElement.scrollTop > 50) {
                    backToTop.style.display = "flex";
                } else {
                    backToTop.style.display = "none";
                }
            }
        };

        /* ==========================================================
        3. SMOOTH SCROLLING
        ========================================================== */
        var pageLinks = document.querySelectorAll('.page-scroll');
        pageLinks.forEach(elem => {
            elem.addEventListener('click', e => {
                e.preventDefault();
                var href = elem.getAttribute('href');
                var targetAnchor = document.querySelector(href);
                if (targetAnchor) {
                    targetAnchor.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start'
                    });
                }
            });
        });

        /* =========================================
        MOBILE MENU LOGIC (FIXED)
        ========================================= */
        const navbarToggler = document.querySelector(".navbar-toggler");
        const navbarCollapse = document.querySelector(".navbar-collapse");
        const navLinks = document.querySelectorAll(".page-scroll");

        if (navbarToggler) {
            navbarToggler.addEventListener("click", function () {
                // Toggle the "active" class for the hamburger animation
                navbarToggler.classList.toggle("active");
                
                // Use Bootstrap's built-in toggle class if you're using BS5
                if (navbarCollapse.classList.contains("show")) {
                    navbarCollapse.classList.remove("show");
                } else {
                    navbarCollapse.classList.add("show");
                }
            });
        }

        // Automatically close menu when a nav link is clicked
        navLinks.forEach((link) => {
            link.addEventListener("click", () => {
                if (navbarToggler.classList.contains("active")) {
                    navbarToggler.classList.remove("active");
                    navbarCollapse.classList.remove("show");
                }
            });
        });

    })();

    var slider = tns({
        container: '.my-slider',
        items: 1,
        slideBy: 'page',
        autoplay: true,
        mouseDrag: true,
        gutter: 0,
        nav: true,
        controls: false,
        responsive: {
            0: {
                items: 1,
            },
            768: {
                items: 2,
            },
            992: {
                items: 3,
            }
        }
    });