package cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import cart.dao.CouponDao;
import cart.domain.Coupon;
import cart.domain.Member;
import cart.domain.vo.Amount;
import cart.exception.BusinessException;
import cart.ui.dto.response.CouponDiscountResponse;
import cart.ui.dto.response.CouponResponse;
import cart.ui.dto.response.PossibleCouponResponse;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    private final Member member = new Member(1L, "test@test.com", "password");
    private final Coupon coupon1 = new Coupon(1L, "name1", Amount.of(1_000), Amount.of(10_000), false);
    private final Coupon coupon2 = new Coupon(2L, "name2", Amount.of(2_000), Amount.of(20_000), false);

    @Mock
    private CouponDao couponDao;
    @InjectMocks
    private CouponService couponService;

    @Test
    @DisplayName("회원별로 쿠폰을 찾는다.")
    void testFindAllCouponByMember() {
        //given
        final List<Coupon> coupons = List.of(coupon1, coupon2);
        given(couponDao.findAll())
            .willReturn(coupons);

        //when
        final List<CouponResponse> couponResponses = couponService.findAllCouponByMember(member);

        //then
        final List<CouponResponse> expectedResponses = coupons.stream()
            .map(this::makeCouponResponse)
            .collect(Collectors.toUnmodifiableList());
        assertThat(couponResponses).usingRecursiveComparison().isEqualTo(expectedResponses);
    }

    private CouponResponse makeCouponResponse(final Coupon coupon) {
        return new CouponResponse(coupon.getId(), coupon.getName(), coupon.getMinAmount().getValue(),
            coupon.getDiscountAmount().getValue(), coupon.isUsed());
    }

    @Test
    @DisplayName("사용자 별 사용 가능한 쿠폰을 반환한다.")
    void testFindPossibleCouponByMember() {
        //given
        final List<Coupon> coupons = List.of(coupon1, coupon2);
        given(couponDao.findAllByMemberWhereIsNotUsed(any(Member.class)))
            .willReturn(coupons);

        //when
        final List<PossibleCouponResponse> responses = couponService.findPossibleCouponByMember(member);

        //then
        final List<PossibleCouponResponse> expectedResponse = coupons.stream()
            .map(this::makePossibleCouponResponse)
            .collect(Collectors.toUnmodifiableList());
        assertThat(responses).usingRecursiveComparison().isEqualTo(expectedResponse);
    }

    private PossibleCouponResponse makePossibleCouponResponse(final Coupon coupon) {
        return new PossibleCouponResponse(coupon.getId(), coupon.getName(), coupon.getMinAmount().getValue());
    }

    @Test
    @DisplayName("쿠폰 할인 금액을 조회한다.")
    void testCalculateCouponDiscount() {
        //given
        given(couponDao.findById(anyLong()))
            .willReturn(Optional.of(coupon1));

        //when
        final CouponDiscountResponse response = couponService.calculateCouponDiscount(1L, 30_000);

        //then
        assertThat(response.getDiscountedProductAmount()).isEqualTo(29_000);
    }

    @Test
    @DisplayName("최소 금액을 넘지 못하는 총 상품 금액이 들어왔을 때, 쿠폰 할인 금액을 조회한다.")
    void testCalculateCouponDiscountWhenTotalProductAmountLess() {
        //given
        given(couponDao.findById(anyLong()))
            .willReturn(Optional.of(coupon1));

        //when
        assertThatThrownBy(() -> couponService.calculateCouponDiscount(1L, 8_000))
            .isInstanceOf(BusinessException.class);
    }
}
