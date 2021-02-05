$(document).ready(function () {
    var index = parent.layer.getFrameIndex(window.name); //获取窗口索引

    parent.$("#parent_receive").text("test");
    console.log("try to send params");

    $('#childBtn').click(function () {
        window.parent.parentFun("child");
        console.log("child clicked");
        parent.$("#parent_receive").text("child clicked");
        parent.layer.close(index);
    });

    //function for parent to call
    window.parent._childFun = {
        childFun: function (param) {
            console.log("child function is called:" + param);
            return 3;
        }
    };

    function addClickListenerForElement(elementId, type) {
        $(document).on('click', elementId, function () {
            $("a[select='selected']").attr('select', 'false');
            $(elementId).attr('select', 'selected');
        });
    }

    window.parent._callbackdata = {
        callBackData: function () {
            return {
                selectedName: $("a[select='selected']").attr('id'),
                selectedNickName: $("a[select='selected']").find('h3').html()
            };
        }
    }


    function showVotePage(data) {
        console.log("show vote page at overlayer");
        let imageUrl = '../img/unknown_icon.png';
        $('.card__background').css('background-image', 'url(' + imageUrl + ')');
        let id = 'testId';
        let test_element = '<div id="' + id + '">dynamic element</div>'
        $("#test").after(test_element);
    }

    function showWitchPage(data) {
        console.log("show witch page at overlayer");
    }

    function showSeerPage(data) {
        console.log("show seer page at overlayer");
    }

    function showWolfPage(killList) {
        console.log("show wolf page at overlayer");
        console.log(killList);
        console.log(killList.length);

        $.each(killList, function (key, player) {
            let imageUrl = '../img/unknown_icon.png';

            let card_background_div = $('<div>').addClass('card__background').css('background-image', 'url(' + imageUrl + ')');
            let card_content_div = $('<div>').addClass('card__content');
            let card__category_p = $('<p>').addClass('card__category').text('Player');
            let card__heading_h3 = $('<h3>').addClass('card__heading').text(player.nickName);
            card_content_div.append(card__category_p).append(card__heading_h3)

            let card = $('<a>').attr('id', player.name)
                .addClass('card')
                .append(card_background_div)
                .append(card_content_div);

            $('.card-grid').append(card);
            let cardId = '#' + player.name;
            addClickListenerForElement(cardId, 'wolf');
        });
    }


    window.parent._showOverlayer = {
        show: function (role, data) {
            // show different page view by role(vote, witch, seer, wolf)
            switch (role) {
                case 'vote':
                    showVotePage(data);
                    break;
                case 'witch':
                    showWitchPage(data);
                    break;
                case 'seer':
                    showSeerPage(data);
                    break;
                case 'wolf':
                    showWolfPage(data);
                    break;
                default:
                    console.log("can't find role:" + role);
            }
        }
    }


});