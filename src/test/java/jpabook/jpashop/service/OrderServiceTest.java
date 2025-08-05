package jpabook.jpashop.service;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.DeliveryStatus;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.Assert.*;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class OrderServiceTest {

    @Autowired EntityManager em;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orderRepository;

    @Test
    public void 상품주문() throws Exception {
        //given
        Member member = createMember();

        Book book = createBook("시골 JPA", 10000, 10);

        int orderCount = 2;

        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findOne(orderId);

        assertEquals("상품 주문시 상태는 ORDER", OrderStatus.ORDER, getOrder.getStatus());
        assertEquals("주문한 상품 종류 수가 정확해야 한다.", 1, getOrder.getOrderItems().size());
        assertEquals("주문 가격은 가격 * 수량이다.", 10000 * orderCount, getOrder.getTotalPrice());
        assertEquals("주문 수량만큼 재고가 줄어야 한다.", 8, book.getStockQuantity());
    }

    @Test(expected = NotEnoughStockException.class)
    public void 상품주문_재고수량초과() throws Exception {
        //given
        Member member = createMember();
        Item item = createBook("시골 JPA", 10000, 10);

        int orderCount = 11;

        //when
        orderService.order(member.getId(), item.getId(), orderCount);

        //then
        fail("재고 수량 부족 예외가 발행해야 한다.");
    }

    @Test
    public void 주문취소() throws Exception {
        //given
        Member member = createMember();
        Book item = createBook("시골 JPA", 10000, 10);

        int orderCount = 2;

        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        //when
        orderService.cancelOrder(orderId);

        //then
        Order getOrder = orderRepository.findOne(orderId);

        assertEquals("주문 취소시 상태는 CANCEL 이다.", OrderStatus.CANCEL, getOrder.getStatus());
        assertEquals("주문이 취소된 상품은 그만큼 재고가 증가해야 한다.", 10, item.getStockQuantity());
    }

    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울", "강가", "123-123"));
        em.persist(member);
        return member;
    }



    @Test
    public void 상품주문_수량0() throws Exception {
        //given
        Member member = createMember();
        Book book = createBook("시골 JPA", 10000, 10);
        int orderCount = 0;

        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertEquals("수량이 0인 주문의 상태는 ORDER", OrderStatus.ORDER, getOrder.getStatus());
        assertEquals("수량이 0인 주문의 총 가격은 0이다.", 0, getOrder.getTotalPrice());
        assertEquals("수량이 0인 주문시 재고는 변하지 않는다.", 10, book.getStockQuantity());
    }

    @Test
    public void 상품주문_최대재고수량() throws Exception {
        //given
        Member member = createMember();
        Book book = createBook("시골 JPA", 10000, 10);
        int orderCount = 10; // 전체 재고

        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertEquals("전체 재고 주문시 상태는 ORDER", OrderStatus.ORDER, getOrder.getStatus());
        assertEquals("전체 재고 주문시 재고는 0이 된다.", 0, book.getStockQuantity());
        assertEquals("전체 재고 주문시 총 가격이 정확해야 한다.", 100000, getOrder.getTotalPrice());
    }

    @Test(expected = IllegalArgumentException.class)
    public void 상품주문_존재하지않는상품() throws Exception {
        //given
        Member member = createMember();
        Long nonExistentItemId = 999L;
        int orderCount = 2;

        //when
        orderService.order(member.getId(), nonExistentItemId, orderCount);

        //then
        fail("존재하지 않는 상품 예외가 발생해야 한다.");
    }

    @Test(expected = NotEnoughStockException.class)
    public void 상품주문_음수수량() throws Exception {
        //given
        Member member = createMember();
        Book book = createBook("시골 JPA", 10000, 10);
        int orderCount = -1;

        //when
        orderService.order(member.getId(), book.getId(), orderCount);

        //then
        fail("음수 수량 예외가 발생해야 한다.");
    }

    @Test
    public void 주문취소_부분주문후취소() throws Exception {
        //given
        Member member = createMember();
        Book item = createBook("시골 JPA", 10000, 10);
        int orderCount = 3;

        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);
        assertEquals("주문 후 재고가 줄어야 한다.", 7, item.getStockQuantity());

        //when
        orderService.cancelOrder(orderId);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertEquals("주문 취소시 상태는 CANCEL 이다.", OrderStatus.CANCEL, getOrder.getStatus());
        assertEquals("주문이 취소된 상품은 원래 재고로 복구되어야 한다.", 10, item.getStockQuantity());
    }

    @Test
    public void 주문취소_이미취소된주문() throws Exception {
        //given
        Member member = createMember();
        Book item = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;

        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);
        orderService.cancelOrder(orderId); // 첫 번째 취소

        //when
        orderService.cancelOrder(orderId); // 두 번째 취소 시도

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertEquals("이미 취소된 주문의 상태는 CANCEL로 유지된다.", OrderStatus.CANCEL, getOrder.getStatus());
        assertEquals("이미 취소된 주문을 다시 취소해도 재고는 변하지 않는다.", 10, item.getStockQuantity());
    }

    @Test
    public void 복수상품주문() throws Exception {
        //given
        Member member = createMember();
        Book book1 = createBook("시골 JPA", 10000, 10);
        Book book2 = createBook("도시 JPA", 20000, 5);
        Book book3 = createBook("바다 JPA", 15000, 8);

        //when - 각각 다른 수량으로 주문
        Long orderId1 = orderService.order(member.getId(), book1.getId(), 2);
        Long orderId2 = orderService.order(member.getId(), book2.getId(), 1);
        Long orderId3 = orderService.order(member.getId(), book3.getId(), 3);

        //then
        Order order1 = orderRepository.findOne(orderId1);
        Order order2 = orderRepository.findOne(orderId2);
        Order order3 = orderRepository.findOne(orderId3);

        assertEquals("첫 번째 주문 총액이 정확해야 한다.", 20000, order1.getTotalPrice());
        assertEquals("두 번째 주문 총액이 정확해야 한다.", 20000, order2.getTotalPrice());
        assertEquals("세 번째 주문 총액이 정확해야 한다.", 45000, order3.getTotalPrice());

        assertEquals("첫 번째 상품 재고가 정확해야 한다.", 8, book1.getStockQuantity());
        assertEquals("두 번째 상품 재고가 정확해야 한다.", 4, book2.getStockQuantity());
        assertEquals("세 번째 상품 재고가 정확해야 한다.", 5, book3.getStockQuantity());
    }

    @Test
    public void 동일상품_연속주문() throws Exception {
        //given
        Member member = createMember();
        Book book = createBook("시골 JPA", 10000, 10);

        //when - 같은 상품을 여러 번 주문
        Long orderId1 = orderService.order(member.getId(), book.getId(), 3);
        Long orderId2 = orderService.order(member.getId(), book.getId(), 2);

        //then
        Order order1 = orderRepository.findOne(orderId1);
        Order order2 = orderRepository.findOne(orderId2);

        assertEquals("첫 번째 주문 총액이 정확해야 한다.", 30000, order1.getTotalPrice());
        assertEquals("두 번째 주문 총액이 정확해야 한다.", 20000, order2.getTotalPrice());
        assertEquals("연속 주문 후 재고가 정확해야 한다.", 5, book.getStockQuantity());
    }

    @Test
    public void 주문후_부분취소_재주문() throws Exception {
        //given
        Member member = createMember();
        Book book = createBook("시골 JPA", 10000, 10);

        //when
        Long orderId1 = orderService.order(member.getId(), book.getId(), 4);
        assertEquals("첫 주문 후 재고", 6, book.getStockQuantity());

        orderService.cancelOrder(orderId1);
        assertEquals("주문 취소 후 재고", 10, book.getStockQuantity());

        Long orderId2 = orderService.order(member.getId(), book.getId(), 2);

        //then
        Order order1 = orderRepository.findOne(orderId1);
        Order order2 = orderRepository.findOne(orderId2);

        assertEquals("취소된 주문의 상태", OrderStatus.CANCEL, order1.getStatus());
        assertEquals("새 주문의 상태", OrderStatus.ORDER, order2.getStatus());
        assertEquals("최종 재고량", 8, book.getStockQuantity());
    }

    @Test
    public void 고가상품주문() throws Exception {
        //given
        Member member = createMember();
        Book expensiveBook = createBook("프리미엄 JPA", 1000000, 1);
        int orderCount = 1;

        //when
        Long orderId = orderService.order(member.getId(), expensiveBook.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertEquals("고가 상품 주문 총액이 정확해야 한다.", 1000000, getOrder.getTotalPrice());
        assertEquals("고가 상품 주문 후 재고가 정확해야 한다.", 0, expensiveBook.getStockQuantity());
    }

    @Test
    public void 여러회원_동일상품주문() throws Exception {
        //given
        Member member1 = createMember();
        Member member2 = createMemberWithName("회원2", "부산");
        Member member3 = createMemberWithName("회원3", "대구");
        Book book = createBook("인기 JPA", 15000, 20);

        //when
        Long orderId1 = orderService.order(member1.getId(), book.getId(), 5);
        Long orderId2 = orderService.order(member2.getId(), book.getId(), 3);
        Long orderId3 = orderService.order(member3.getId(), book.getId(), 2);

        //then
        Order order1 = orderRepository.findOne(orderId1);
        Order order2 = orderRepository.findOne(orderId2);
        Order order3 = orderRepository.findOne(orderId3);

        assertEquals("첫 번째 회원 주문 총액", 75000, order1.getTotalPrice());
        assertEquals("두 번째 회원 주문 총액", 45000, order2.getTotalPrice());
        assertEquals("세 번째 회원 주문 총액", 30000, order3.getTotalPrice());
        assertEquals("모든 주문 후 재고량", 10, book.getStockQuantity());
    }

    @Test
    public void 주문_배송정보확인() throws Exception {
        //given
        Member member = createMember();
        Book book = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;

        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertNotNull("주문에 배송정보가 있어야 한다.", getOrder.getDelivery());
        assertEquals("배송 주소가 회원 주소와 같아야 한다.", member.getAddress(), getOrder.getDelivery().getAddress());
    }

    @Test
    public void 상품주문_극한재고테스트() throws Exception {
        //given
        Member member = createMember();
        Book book = createBook("한정판 JPA", 50000, 1);
        int orderCount = 1;

        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertEquals("한정판 상품 주문 상태", OrderStatus.ORDER, getOrder.getStatus());
        assertEquals("한정판 상품 주문 후 재고", 0, book.getStockQuantity());
        assertEquals("한정판 상품 총액", 50000, getOrder.getTotalPrice());
    }

    @Test
    public void 대량주문_테스트() throws Exception {
        //given
        Member member = createMember();
        Book book = createBook("대량 JPA", 1000, 1000);
        int orderCount = 500;

        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        assertEquals("대량 주문 상태", OrderStatus.ORDER, getOrder.getStatus());
        assertEquals("대량 주문 후 재고", 500, book.getStockQuantity());
        assertEquals("대량 주문 총액", 500000, getOrder.getTotalPrice());
    }

    private Book createBookWithAuthor(String name, String author, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setAuthor(author);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMemberWithName(String name, String city) {
        Member member = new Member();
        member.setName(name);
        member.setAddress(new Address(city, "강가", "123-123"));
        em.persist(member);
        return member;
    }
}