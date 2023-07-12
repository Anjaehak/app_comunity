package com.company.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.company.exception.NotExistPostException;
import com.company.exception.NotExistUserException;
import com.company.model.dto.request.CreatePostRequest;
import com.company.model.dto.request.PostLikeRequest;
import com.company.model.dto.request.UpdatePostRequest;
import com.company.model.entity.Image;
import com.company.model.entity.Recommend;
import com.company.model.entity.Post;
import com.company.model.entity.User;
import com.company.repository.ImageRepository;
import com.company.repository.RecommendRepository;
import com.company.repository.PostRepository;
import com.company.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService {

	@Value("${upload.server}")
	String uploadServer;
	@Value("${upload.basedir}")
	String uploadBaseDir;

	private final PostRepository postRepository;
	private final UserRepository userRepository;
	private final ImageRepository imageRepository;
	private final RecommendRepository likeRepository;

	public List<Post> allPosts() {
		return postRepository.findAll();

	}

	public void save(String principal, CreatePostRequest req)
			throws NotExistUserException, IllegalStateException, IOException {
		User user = userRepository.findByEmail(principal).orElseThrow(() -> new NotExistUserException());
		Post post = new Post();
		LocalDateTime currentTime = LocalDateTime.now();
		post.setPostContent(req.getPostContent());
		post.setPostDate(currentTime);
		post.setTitle(req.getTitle());
		post.setPostWriter(user);

		var saved = postRepository.save(post);

		if (req.getAttaches() != null) { // 파일이 넘어왔다면
			File uploadDirectory = new File(uploadBaseDir + "/feed/" + saved.getId());
			uploadDirectory.mkdirs();

			for (MultipartFile multi : req.getAttaches()) { // 하나씩 반복문 돌면서
				// 어디다가 file 옮겨둘껀지 File 객체로 정의하고
				String fileName = String.valueOf(System.currentTimeMillis());
				String extension = multi.getOriginalFilename().split("\\.")[1];
				File dest = new File(uploadDirectory, fileName + "." + extension);

				multi.transferTo(dest); // 옮기는걸 진행

				// 업로드가 끝나면 DB에 기록
				Image image = new Image();
				// 업로드를 한 곳이 어디냐에 따라서 결정이 되는 값
				image.setImageUrl(uploadServer + "/resource/feed/" + saved.getId() + "/" + fileName + "." + extension);
				image.setPostsId(saved);

				imageRepository.save(image);

			}
		}
	}

	public void update(String principal, UpdatePostRequest req) throws NotExistUserException, NotExistPostException {
		User user = userRepository.findByEmail(principal).orElseThrow(() -> new NotExistUserException());
		System.out.println("user id =" + user.getId());
		Integer id = req.getId();

		Post post = postRepository.findById(id).orElseThrow(() -> new NotExistPostException());
		LocalDateTime currentTime = LocalDateTime.now();
		post.setTitle(req.getTitle());
		post.setPostDate(currentTime);
		post.setPostContent(req.getPostContent());

		postRepository.save(post);

	}

	public void recommendPost(String email, Integer postNumber) throws NotExistUserException, NotExistPostException {

		User user = userRepository.findByEmail(email).orElseThrow(() -> new NotExistUserException());
		Post post = postRepository.findById(postNumber).orElseThrow(() -> new NotExistPostException());
		
		Recommend like =Recommend.builder()
				.userId(user)
				.postId(post)
				.build();
		likeRepository.save(like);
		

	}



}
