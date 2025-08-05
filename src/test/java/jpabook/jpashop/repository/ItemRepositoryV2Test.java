package jpabook.jpashop.repository;

import jpabook.jpashop.domain.item.Album;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.domain.item.Movie;
import jpabook.jpashop.exception.NotEnoughStockException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * ItemRepositoryV2에 대한 포괄적인 단위 테스트
 * 테스팅 프레임워크: JUnit 4 with Spring Boot Test
 * 어설션 라이브러리: JUnit Assert
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class ItemRepositoryV2Test {

    @Autowired
    ItemRepositoryV2 itemRepositoryV2;
    
    @Autowired
    EntityManager em;

    @Test
    public void 아이템_저장() throws Exception {
        //given
        Book book = new Book();
        book.setName("JPA 프로그래밍");
        book.setPrice(25000);
        book.setStockQuantity(100);
        book.setAuthor("김영한");
        book.setIsbn("978-89-6626-362-1");

        //when
        Item savedItem = itemRepositoryV2.save(book);

        //then
        assertNotNull(savedItem.getId());
        assertEquals("JPA 프로그래밍", savedItem.getName());
        assertEquals(25000, savedItem.getPrice());
        assertEquals(100, savedItem.getStockQuantity());
        assertTrue(savedItem instanceof Book);
        
        Book savedBook = (Book) savedItem;
        assertEquals("김영한", savedBook.getAuthor());
        assertEquals("978-89-6626-362-1", savedBook.getIsbn());
    }

    @Test
    public void 아이템_조회() throws Exception {
        //given
        Book book = new Book();
        book.setName("Spring Boot 실전활용");
        book.setPrice(30000);
        book.setStockQuantity(50);
        book.setAuthor("백기선");
        book.setIsbn("978-89-6626-999-9");
        
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();
        em.clear();

        //when
        Optional<Item> foundItemOpt = itemRepositoryV2.findById(savedItem.getId());

        //then
        assertTrue(foundItemOpt.isPresent());
        Item foundItem = foundItemOpt.get();
        assertEquals(savedItem.getId(), foundItem.getId());
        assertEquals("Spring Boot 실전활용", foundItem.getName());
        assertEquals(30000, foundItem.getPrice());
        assertEquals(50, foundItem.getStockQuantity());
    }

    @Test
    public void 존재하지_않는_아이템_조회() throws Exception {
        //given
        Long nonExistentId = 999999L;

        //when
        Optional<Item> foundItem = itemRepositoryV2.findById(nonExistentId);

        //then
        assertFalse(foundItem.isPresent());
    }

    @Test
    public void 모든_아이템_조회() throws Exception {
        //given
        Book book = createBook("테스트 책", 10000, 100);
        Movie movie = createMovie("테스트 영화", 15000, 50);
        Album album = createAlbum("테스트 앨범", 20000, 30);
        
        itemRepositoryV2.save(book);
        itemRepositoryV2.save(movie);
        itemRepositoryV2.save(album);
        em.flush();

        //when
        List<Item> allItems = itemRepositoryV2.findAll();

        //then
        assertEquals(3, allItems.size());
        
        // 타입별 개수 확인
        long bookCount = allItems.stream().filter(item -> item instanceof Book).count();
        long movieCount = allItems.stream().filter(item -> item instanceof Movie).count();
        long albumCount = allItems.stream().filter(item -> item instanceof Album).count();
        
        assertEquals(1, bookCount);
        assertEquals(1, movieCount);
        assertEquals(1, albumCount);
    }

    @Test
    public void 빈_테이블_모든_아이템_조회() throws Exception {
        //when
        List<Item> allItems = itemRepositoryV2.findAll();

        //then
        assertTrue(allItems.isEmpty());
        assertEquals(0, allItems.size());
    }

    @Test
    public void 아이템_수정() throws Exception {
        //given
        Book book = createBook("원본 책", 10000, 100);
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();
        em.clear();

        //when
        Optional<Item> foundItemOpt = itemRepositoryV2.findById(savedItem.getId());
        assertTrue(foundItemOpt.isPresent());
        
        Item foundItem = foundItemOpt.get();
        foundItem.setName("수정된 책");
        foundItem.setPrice(15000);
        foundItem.setStockQuantity(200);
        
        Item updatedItem = itemRepositoryV2.save(foundItem);
        em.flush();

        //then
        assertEquals("수정된 책", updatedItem.getName());
        assertEquals(15000, updatedItem.getPrice());
        assertEquals(200, updatedItem.getStockQuantity());
    }

    @Test
    public void 아이템_삭제() throws Exception {
        //given
        Book book = createBook("삭제할 책", 10000, 50);
        Item savedItem = itemRepositoryV2.save(book);
        Long itemId = savedItem.getId();
        em.flush();

        //when
        itemRepositoryV2.deleteById(itemId);
        em.flush();

        //then
        Optional<Item> deletedItem = itemRepositoryV2.findById(itemId);
        assertFalse(deletedItem.isPresent());
    }

    @Test
    public void 아이템_존재_여부_확인() throws Exception {
        //given
        Book book = createBook("존재 확인 책", 10000, 50);
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();

        //when & then
        assertTrue(itemRepositoryV2.existsById(savedItem.getId()));
        assertFalse(itemRepositoryV2.existsById(999999L));
    }

    @Test
    public void 아이템_개수_조회() throws Exception {
        //given
        Book book1 = createBook("책1", 10000, 50);
        Book book2 = createBook("책2", 15000, 30);
        Movie movie = createMovie("영화1", 20000, 40);
        
        itemRepositoryV2.save(book1);
        itemRepositoryV2.save(book2);
        itemRepositoryV2.save(movie);
        em.flush();

        //when
        long count = itemRepositoryV2.count();

        //then
        assertEquals(3, count);
    }

    @Test
    public void 영화_아이템_저장_및_조회() throws Exception {
        //given
        Movie movie = new Movie();
        movie.setName("아바타");
        movie.setPrice(12000);
        movie.setStockQuantity(200);
        movie.setDirector("제임스 카메론");
        movie.setActor("샘 워딩턴");

        //when
        Item savedItem = itemRepositoryV2.save(movie);
        em.flush();
        em.clear();

        //then
        Optional<Item> foundItemOpt = itemRepositoryV2.findById(savedItem.getId());
        assertTrue(foundItemOpt.isPresent());
        
        Item foundItem = foundItemOpt.get();
        assertTrue(foundItem instanceof Movie);
        
        Movie foundMovie = (Movie) foundItem;
        assertEquals("아바타", foundMovie.getName());
        assertEquals("제임스 카메론", foundMovie.getDirector());
        assertEquals("샘 워딩턴", foundMovie.getActor());
    }

    @Test
    public void 앨범_아이템_저장_및_조회() throws Exception {
        //given
        Album album = new Album();
        album.setName("좋은 날");
        album.setPrice(8000);
        album.setStockQuantity(150);
        album.setArtist("IU");
        album.setEtc("로엔엔터테인먼트");

        //when
        Item savedItem = itemRepositoryV2.save(album);
        em.flush();
        em.clear();

        //then
        Optional<Item> foundItemOpt = itemRepositoryV2.findById(savedItem.getId());
        assertTrue(foundItemOpt.isPresent());
        
        Item foundItem = foundItemOpt.get();
        assertTrue(foundItem instanceof Album);
        
        Album foundAlbum = (Album) foundItem;
        assertEquals("좋은 날", foundAlbum.getName());
        assertEquals("IU", foundAlbum.getArtist());
        assertEquals("로엔엔터테인먼트", foundAlbum.getEtc());
    }

    @Test
    public void 페이징_조회() throws Exception {
        //given
        for (int i = 1; i <= 10; i++) {
            Book book = createBook("책" + i, 10000 + i, 50 + i);
            itemRepositoryV2.save(book);
        }
        em.flush();

        //when
        Pageable pageable = PageRequest.of(0, 3);
        Page<Item> itemPage = itemRepositoryV2.findAll(pageable);

        //then
        assertEquals(3, itemPage.getContent().size());
        assertEquals(10, itemPage.getTotalElements());
        assertEquals(4, itemPage.getTotalPages());
        assertEquals(0, itemPage.getNumber());
        assertTrue(itemPage.hasNext());
        assertFalse(itemPage.hasPrevious());
    }

    @Test
    public void 정렬_조회() throws Exception {
        //given
        Book book1 = createBook("C책", 30000, 100);
        Book book2 = createBook("A책", 10000, 200);
        Book book3 = createBook("B책", 20000, 150);
        
        itemRepositoryV2.save(book1);
        itemRepositoryV2.save(book2);
        itemRepositoryV2.save(book3);
        em.flush();

        //when
        Sort sort = Sort.by(Sort.Direction.ASC, "name");
        List<Item> sortedItems = itemRepositoryV2.findAll(sort);

        //then
        assertEquals(3, sortedItems.size());
        assertEquals("A책", sortedItems.get(0).getName());
        assertEquals("B책", sortedItems.get(1).getName());
        assertEquals("C책", sortedItems.get(2).getName());
    }

    @Test
    public void 가격순_정렬_조회() throws Exception {
        //given
        Book book1 = createBook("비싼책", 50000, 10);
        Book book2 = createBook("저렴한책", 5000, 100);
        Book book3 = createBook("중간책", 25000, 50);
        
        itemRepositoryV2.save(book1);
        itemRepositoryV2.save(book2);
        itemRepositoryV2.save(book3);
        em.flush();

        //when
        Sort sort = Sort.by(Sort.Direction.ASC, "price");
        List<Item> sortedItems = itemRepositoryV2.findAll(sort);

        //then
        assertEquals(3, sortedItems.size());
        assertEquals(5000, sortedItems.get(0).getPrice());
        assertEquals(25000, sortedItems.get(1).getPrice());
        assertEquals(50000, sortedItems.get(2).getPrice());
    }

    @Test
    public void 여러_아이템_일괄_저장() throws Exception {
        //given
        Book book1 = createBook("책1", 10000, 50);
        Book book2 = createBook("책2", 15000, 30);
        Movie movie = createMovie("영화1", 12000, 40);
        
        List<Item> items = List.of(book1, book2, movie);

        //when
        List<Item> savedItems = itemRepositoryV2.saveAll(items);

        //then
        assertEquals(3, savedItems.size());
        for (Item item : savedItems) {
            assertNotNull(item.getId());
        }
    }

    @Test
    public void 여러_아이템_일괄_삭제() throws Exception {
        //given
        Book book1 = createBook("책1", 10000, 50);
        Book book2 = createBook("책2", 15000, 30);
        Movie movie = createMovie("영화1", 12000, 40);
        
        Item savedBook1 = itemRepositoryV2.save(book1);
        Item savedBook2 = itemRepositoryV2.save(book2);
        Item savedMovie = itemRepositoryV2.save(movie);
        em.flush();
        
        List<Long> idsToDelete = List.of(savedBook1.getId(), savedBook2.getId());

        //when
        itemRepositoryV2.deleteAllById(idsToDelete);
        em.flush();

        //then
        assertEquals(1, itemRepositoryV2.count());
        assertTrue(itemRepositoryV2.existsById(savedMovie.getId()));
        assertFalse(itemRepositoryV2.existsById(savedBook1.getId()));
        assertFalse(itemRepositoryV2.existsById(savedBook2.getId()));
    }

    @Test
    public void 재고_증가_테스트() throws Exception {
        //given
        Book book = createBook("재고테스트책", 10000, 50);
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();
        em.clear();

        //when
        Item foundItem = itemRepositoryV2.findById(savedItem.getId()).orElseThrow();
        foundItem.addStock(25);
        itemRepositoryV2.save(foundItem);
        em.flush();

        //then
        Item updatedItem = itemRepositoryV2.findById(savedItem.getId()).orElseThrow();
        assertEquals(75, updatedItem.getStockQuantity());
    }

    @Test
    public void 재고_감소_테스트() throws Exception {
        //given
        Book book = createBook("재고감소테스트책", 10000, 100);
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();
        em.clear();

        //when
        Item foundItem = itemRepositoryV2.findById(savedItem.getId()).orElseThrow();
        foundItem.removeStock(30);
        itemRepositoryV2.save(foundItem);
        em.flush();

        //then
        Item updatedItem = itemRepositoryV2.findById(savedItem.getId()).orElseThrow();
        assertEquals(70, updatedItem.getStockQuantity());
    }

    @Test(expected = NotEnoughStockException.class)
    public void 재고_부족_예외_테스트() throws Exception {
        //given
        Book book = createBook("재고부족테스트책", 10000, 10);
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();

        //when
        Item foundItem = itemRepositoryV2.findById(savedItem.getId()).orElseThrow();
        foundItem.removeStock(20); // 재고보다 많이 감소 시도

        //then
        // NotEnoughStockException 발생 예상
    }

    @Test
    public void 경계값_테스트_재고_0() throws Exception {
        //given
        Book book = createBook("경계값테스트책", 10000, 0);

        //when
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();

        //then
        assertEquals(0, savedItem.getStockQuantity());
        
        // 재고 0에서 추가
        savedItem.addStock(10);
        assertEquals(10, savedItem.getStockQuantity());
    }

    @Test
    public void 경계값_테스트_가격_0() throws Exception {
        //given
        Book book = createBook("무료책", 0, 100);

        //when
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();

        //then
        assertEquals(0, savedItem.getPrice());
    }

    @Test
    public void 큰_수량_처리_테스트() throws Exception {
        //given
        Book book = createBook("대량재고책", 10000, Integer.MAX_VALUE - 1000);

        //when
        Item savedItem = itemRepositoryV2.save(book);
        em.flush();

        //then
        assertEquals(Integer.MAX_VALUE - 1000, savedItem.getStockQuantity());
        
        // 재고 추가시 오버플로우 체크
        savedItem.addStock(500);
        assertEquals(Integer.MAX_VALUE - 500, savedItem.getStockQuantity());
    }

    @Test
    public void 동일한_이름_다른_타입_아이템_테스트() throws Exception {
        //given
        String sameName = "인기상품";
        Book book = createBook(sameName, 10000, 50);
        Movie movie = createMovie(sameName, 15000, 30);
        Album album = createAlbum(sameName, 8000, 100);

        //when
        Item savedBook = itemRepositoryV2.save(book);
        Item savedMovie = itemRepositoryV2.save(movie);
        Item savedAlbum = itemRepositoryV2.save(album);
        em.flush();

        //then
        List<Item> allItems = itemRepositoryV2.findAll();
        assertEquals(3, allItems.size());
        
        for (Item item : allItems) {
            assertEquals(sameName, item.getName());
        }
        
        // 각각 다른 타입인지 확인
        assertNotEquals(savedBook.getClass(), savedMovie.getClass());
        assertNotEquals(savedMovie.getClass(), savedAlbum.getClass());
        assertNotEquals(savedAlbum.getClass(), savedBook.getClass());
    }

    // 테스트 헬퍼 메소드들
    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        book.setAuthor("테스트 저자");
        book.setIsbn("TEST-ISBN");
        return book;
    }

    private Movie createMovie(String name, int price, int stockQuantity) {
        Movie movie = new Movie();
        movie.setName(name);
        movie.setPrice(price);
        movie.setStockQuantity(stockQuantity);
        movie.setDirector("테스트 감독");
        movie.setActor("테스트 배우");
        return movie;
    }

    private Album createAlbum(String name, int price, int stockQuantity) {
        Album album = new Album();
        album.setName(name);
        album.setPrice(price);
        album.setStockQuantity(stockQuantity);
        album.setArtist("테스트 아티스트");
        album.setEtc("테스트 레이블");
        return album;
    }
}